package com.example.emergencysos.Models;

public class EmergencyContact {
    private String name;
    private String phone;
    private String relation;

    public EmergencyContact(String name, String phone, String relation) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getRelation() { return relation; }
}
