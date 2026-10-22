package com.kce.leave.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "students")
public class Student {
    @Id private String id;
    private String name;          // login username
    private String registerNo;    // login password (stored hashed)
    private String className;     // e.g. II-CSE-A
    private String passwordHash;

    public Student() {}
    public Student(String name, String registerNo, String className, String passwordHash) {
        this.name = name; this.registerNo = registerNo; this.className = className; this.passwordHash = passwordHash;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
