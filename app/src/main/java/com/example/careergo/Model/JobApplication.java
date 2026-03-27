package com.example.careergo.Model;

public class JobApplication {
    public JobApplication(String applicationId, String jobId, String studentId, String studentName, String studentEmail, String studentPhone, long appliedDate, String status, String jobTitle, String companyName, String companyImageUrl, String location, String salary) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.studentPhone = studentPhone;
        this.appliedDate = appliedDate;
        this.status = status;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.companyImageUrl = companyImageUrl;
        this.location = location;
        this.salary = salary;
    }
    public JobApplication() {

    }

    private String applicationId;
    private String jobId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private long appliedDate;
    private String status;

    // Additional fields from job
    private String jobTitle;
    private String companyName;
    private String companyImageUrl;
    private String location;
    private String salary;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentPhone() {
        return studentPhone;
    }

    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }

    public long getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(long appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyImageUrl() {
        return companyImageUrl;
    }

    public void setCompanyImageUrl(String companyImageUrl) {
        this.companyImageUrl = companyImageUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }
// Constructors, getters, and setters
}