package com.example.careergo.Model;

// com.example.careergo.Model.User

public class User {
    public User(String firstName, String lastName, String mobileNo, String email, String address, String country ,String city, String role, String cvBase64, boolean approved) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobileNo = mobileNo;
        this.email = email;
        this.address = address;
        this.city = city;
        this.role = role;
        this.country = country;
        this.cvBase64 = cvBase64;
        this.approved = approved;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }



    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getCvBase64() {
        return cvBase64;
    }

    public void setCvBase64(String cvBase64) {
        this.cvBase64 = cvBase64;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String firstName;
    public String lastName;
    public String mobileNo;
    public String email;
    public String address;
    public String city;
    public String role;
    public String profileImageUrl;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String country;
    public String cvBase64; // <--- Store CV as

    public String getSapId() {
        return sapId;
    }

    public void setSapId(String sapId) {
        this.sapId = sapId;
    }

    private String sapId; // Add this field


    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public boolean isHasResume() {
        return hasResume;
    }

    public void setHasResume(boolean hasResume) {
        this.hasResume = hasResume;
    }

    private String resumeUrl; // New field for resume URL

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private String id;

    private boolean hasResume; // New field to track if user has resume


    public  boolean approved;

    public User() {} // Default constructor for Firebase

}
