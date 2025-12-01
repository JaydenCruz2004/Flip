package com.example.flip.model;

public class User {

    public static final String E_KEY = "EMAILV";
    public static final String P_KEY = "PASSWORDV";

    private String email;
    private String pass;
    private String name;


    public User() {

    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPass() {
        return pass;
    }
    public String getName() {
        return name;
    }
}
