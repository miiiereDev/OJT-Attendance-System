package org.groupfive.siomai.model;

/**
 * Subclass of Person representing an Administrator.
 * Showcases Inheritance.
 */
public class Admin extends Person {
    private String username;
    private String password;

    public Admin() {
        super();
    }

    public Admin(int id, String fullName, String username, String password) {
        super(id, fullName);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
