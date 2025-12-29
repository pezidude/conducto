package com.example.conducto2;

public class User {
    private String fname;
    private String lname;
    private  String email;


    public void setFname(String name) {
        this.fname = name;
    }
    public void setLname(String name) {
        this.lname = name;
    }
    public void setEmail(String email) {
        this.email = email;
    }


    public User(String email, String fname, String lname) {
        this.fname = fname;
        this.lname = lname;
        this.email = email;

    }


    public User() {

    }

    @Override
    public String toString() {
        return "User{" +
                "fname='" + fname + '\'' +
                ", lname='" + lname + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getEmail() {
        return email;
    }

}
