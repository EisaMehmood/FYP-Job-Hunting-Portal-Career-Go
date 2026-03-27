package com.example.careergo.Model;

public class UserModel {
    public String uid, fname, email, role;

    public UserModel() { }

    public UserModel(String uid, String fname, String email, String role) {
        this.uid = uid;
        this.fname = fname;
        this.email = email;
        this.role = role;
    }
}
