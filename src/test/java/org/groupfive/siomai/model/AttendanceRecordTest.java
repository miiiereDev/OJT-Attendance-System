package org.groupfive.siomai.model;

import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;

public class AttendanceRecordTest {

    @Test
    public void testCalculateWorkHours() {
        AttendanceRecord record = new AttendanceRecord();
        
        // Setup clockIn: 2026-06-12 08:00:00
        Timestamp in = Timestamp.valueOf("2026-06-12 08:00:00");
        record.setClockIn(in);

        // Case 1: Null clockOut should return 0.0
        assertEquals(0.0, record.calculateWorkHours());

        // Case 2: Clock out 8 hours later (16:00:00)
        Timestamp out = Timestamp.valueOf("2026-06-12 16:00:00");
        record.setClockOut(out);
        assertEquals(8.0, record.getWorkHours());

        // Case 3: Clock out 8.5 hours later (16:30:00)
        out = Timestamp.valueOf("2026-06-12 16:30:00");
        record.setClockOut(out);
        assertEquals(8.5, record.getWorkHours());

        // Case 4: Clock out 8 hours 15 minutes later (16:15:00) -> 8.25 hours
        out = Timestamp.valueOf("2026-06-12 16:15:00");
        record.setClockOut(out);
        assertEquals(8.25, record.getWorkHours());

        // Case 5: Clock out before clock in (invalid sequence)
        out = Timestamp.valueOf("2026-06-12 07:00:00");
        record.setClockOut(out);
        assertEquals(0.0, record.getWorkHours());
    }
}
