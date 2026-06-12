package org.groupfive.siomai.database;

import org.groupfive.siomai.model.Admin;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Decorator implementing caching, background cache polling, 
 * asynchronous writes, and double-check validation overrides.
 */
public class CachedDatabaseOperations implements DatabaseOperations {
    private final DatabaseOperations delegate;
    
    private volatile long lastSyncTime;
    private volatile boolean isSyncing = false;
    private long lastForcedRefreshTime = 0;
    private static final long COOLDOWN_MS = 5000; // 5 seconds
    
    // Caches
    private final Map<String, Admin> adminCache = new ConcurrentHashMap<>();
    private volatile List<Employee> allEmployeesCache = null;
    private final Map<Integer, Employee> employeeByIdCache = new ConcurrentHashMap<>();
    private final Map<String, Employee> employeeByCodeCache = new ConcurrentHashMap<>();
    private final Map<String, String> dailyCodeCache = new ConcurrentHashMap<>(); // key: empId_date
    private final Map<String, AttendanceRecord> todayAttendanceCache = new ConcurrentHashMap<>(); // key: empId_date
    private final Map<Integer, List<AttendanceRecord>> employeeLogsCache = new ConcurrentHashMap<>();
    private final Map<String, List<AttendanceRecord>> logsByDateCache = new ConcurrentHashMap<>(); // key: date string

    private final ScheduledExecutorService scheduler;

    public CachedDatabaseOperations(DatabaseOperations delegate) {
        this.delegate = delegate;
        this.lastSyncTime = System.currentTimeMillis();
        
        // Preheat cache instantly in a background thread
        CompletableFuture.runAsync(this::syncAllCache);
        
        // Poller executing every 30 seconds
        this.scheduler = Executors.newScheduledThreadPool(1, runnable -> {
            Thread t = new Thread(runnable, "CachePollerThread");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::syncAllCache, 30, 30, TimeUnit.SECONDS);
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public boolean isSyncing() {
        return isSyncing;
    }
    
    public synchronized boolean forceRefresh() {
        long now = System.currentTimeMillis();
        if (now - lastForcedRefreshTime >= COOLDOWN_MS) {
            lastForcedRefreshTime = now;
            CompletableFuture.runAsync(this::syncAllCache);
            return true;
        }
        return false;
    }
    
    private synchronized void syncAllCache() {
        if (isSyncing) return;
        isSyncing = true;
        try {
            List<Employee> fresh = delegate.getAllEmployees();
            allEmployeesCache = fresh;
            
            employeeByIdCache.clear();
            employeeByCodeCache.clear();
            for (Employee e : fresh) {
                employeeByIdCache.put(e.getId(), e);
                employeeByCodeCache.put(e.getEmployeeCode(), e);
            }
            
            // Preheat today's daily codes
            Date today = new Date(System.currentTimeMillis());
            for (Employee e : fresh) {
                if (e.isActive()) {
                    String code = delegate.getEmployeeDailyCode(e.getId(), today);
                    if (code != null) {
                        dailyCodeCache.put(e.getId() + "_" + today.toString(), code);
                    }
                }
            }
            
            lastSyncTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("Background cache synchronization failed: " + e.getMessage());
        } finally {
            isSyncing = false;
        }
    }
    
    // --- Admin Operations ---
    @Override
    public Admin getAdminByUsername(String username) throws SQLException {
        Admin admin = adminCache.get(username);
        if (admin == null) {
            admin = delegate.getAdminByUsername(username);
            if (admin != null) {
                adminCache.put(username, admin);
            }
        }
        return admin;
    }
    
    // --- Employee Operations ---
    @Override
    public void addEmployee(Employee employee) throws SQLException {
        delegate.addEmployee(employee);
        allEmployeesCache = null;
        CompletableFuture.runAsync(this::syncAllCache);
    }
    
    @Override
    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> list = allEmployeesCache;
        if (list == null) {
            list = delegate.getAllEmployees();
            allEmployeesCache = list;
            for (Employee e : list) {
                employeeByIdCache.put(e.getId(), e);
                employeeByCodeCache.put(e.getEmployeeCode(), e);
            }
        }
        return list;
    }
    
    @Override
    public Employee getEmployeeById(int id) throws SQLException {
        Employee emp = employeeByIdCache.get(id);
        if (emp == null) {
            emp = delegate.getEmployeeById(id);
            if (emp != null) {
                employeeByIdCache.put(id, emp);
            }
        }
        return emp;
    }
    
    @Override
    public Employee getEmployeeByCode(String code) throws SQLException {
        Employee emp = employeeByCodeCache.get(code);
        if (emp == null) {
            emp = delegate.getEmployeeByCode(code);
            if (emp != null) {
                employeeByCodeCache.put(code, emp);
            }
        }
        return emp;
    }
    
    @Override
    public List<Employee> searchEmployees(String query) throws SQLException {
        List<Employee> all = allEmployeesCache;
        if (all != null) {
            String q = query.toLowerCase().trim();
            List<Employee> matches = new ArrayList<>();
            for (Employee e : all) {
                if (e.getFullName().toLowerCase().contains(q) || e.getEmployeeCode().toLowerCase().contains(q)) {
                    matches.add(e);
                }
            }
            return matches;
        }
        return delegate.searchEmployees(query);
    }
    
    @Override
    public void updateEmployee(Employee employee) throws SQLException {
        delegate.updateEmployee(employee);
        allEmployeesCache = null;
        CompletableFuture.runAsync(this::syncAllCache);
    }
    
    @Override
    public void deleteEmployee(int id) throws SQLException {
        delegate.deleteEmployee(id);
        allEmployeesCache = null;
        CompletableFuture.runAsync(this::syncAllCache);
    }
    
    // --- Daily Codes ---
    @Override
    public String getDailyCode(Date date) throws SQLException {
        return delegate.getDailyCode(date);
    }
    
    @Override
    public void setDailyCode(String code, Date date) throws SQLException {
        delegate.setDailyCode(code, date);
    }
    
    // --- Per-Employee Daily Codes ---
    @Override
    public String getEmployeeDailyCode(int employeeId, Date date) throws SQLException {
        String key = employeeId + "_" + date.toString();
        String code = dailyCodeCache.get(key);
        if (code == null) {
            code = delegate.getEmployeeDailyCode(employeeId, date);
            if (code != null) {
                dailyCodeCache.put(key, code);
            }
        }
        return code;
    }
    
    public String getEmployeeDailyCodeDirect(int employeeId, Date date) throws SQLException {
        String code = delegate.getEmployeeDailyCode(employeeId, date);
        if (code != null) {
            String key = employeeId + "_" + date.toString();
            dailyCodeCache.put(key, code);
        }
        return code;
    }
    
    @Override
    public void setEmployeeDailyCode(int employeeId, String code, Date date) throws SQLException {
        delegate.setEmployeeDailyCode(employeeId, code, date);
        String key = employeeId + "_" + date.toString();
        dailyCodeCache.put(key, code);
    }
    
    @Override
    public List<AttendanceRecord> getAttendanceLogsForEmployee(int employeeId) throws SQLException {
        List<AttendanceRecord> list = employeeLogsCache.get(employeeId);
        if (list == null) {
            list = delegate.getAttendanceLogsForEmployee(employeeId);
            employeeLogsCache.put(employeeId, list);
        }
        return list;
    }
    
    // --- Attendance Logs ---
    @Override
    public AttendanceRecord getTodayAttendanceRecord(int employeeId, Date date) throws SQLException {
        String key = employeeId + "_" + date.toString();
        AttendanceRecord record = todayAttendanceCache.get(key);
        if (record == null) {
            record = delegate.getTodayAttendanceRecord(employeeId, date);
            if (record != null) {
                todayAttendanceCache.put(key, record);
            }
        }
        return record;
    }
    
    @Override
    public void addAttendanceRecord(AttendanceRecord record) throws SQLException {
        // 1. Instantly cache/invalidate locally
        String key = record.getEmployeeId() + "_" + record.getLogDate().toString();
        todayAttendanceCache.put(key, record);
        employeeLogsCache.remove(record.getEmployeeId());
        logsByDateCache.clear();
        
        // 2. Async write
        CompletableFuture.runAsync(() -> {
            try {
                delegate.addAttendanceRecord(record);
            } catch (SQLException e) {
                System.err.println("Async addAttendanceRecord failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public void updateAttendanceRecord(AttendanceRecord record) throws SQLException {
        // 1. Instantly cache/invalidate locally
        String key = record.getEmployeeId() + "_" + record.getLogDate().toString();
        todayAttendanceCache.put(key, record);
        employeeLogsCache.remove(record.getEmployeeId());
        logsByDateCache.clear();
        
        // 2. Async write
        CompletableFuture.runAsync(() -> {
            try {
                delegate.updateAttendanceRecord(record);
            } catch (SQLException e) {
                System.err.println("Async updateAttendanceRecord failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public List<AttendanceRecord> getAttendanceLogsForDate(Date date) throws SQLException {
        String key = date.toString();
        List<AttendanceRecord> list = logsByDateCache.get(key);
        if (list == null) {
            list = delegate.getAttendanceLogsForDate(date);
            logsByDateCache.put(key, list);
        }
        return list;
    }
}
