package com.kce.leave.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "teachers")
public class Teacher {
    @Id private String id;
    private String className;     // e.g. II-CSE-A
    private String passwordHash;

    public Teacher() {}
    public Teacher(String className, String passwordHash) { this.className = className; this.passwordHash = passwordHash; }
    public String getId() { return id; }
    public String getClassName() { return className; }
    public String getPasswordHash() { return passwordHash; }
}
