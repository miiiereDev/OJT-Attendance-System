package org.groupfive.siomai.model;

/**
 * Abstract base class representing a Person.
 * Showcases Encapsulation and Inheritance.
 */
public abstract class Person {
    private int id;
    private String fullName;

    public Person() {
    }

    public Person(int id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
