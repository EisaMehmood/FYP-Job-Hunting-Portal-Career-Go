package com.example.careergo.Model;

import java.util.List;

public class Job {
    public String getJobCategory() {
        return JobCategory;
    }

    public void setJobCategory(String jobCategory) {
        JobCategory = jobCategory;
    }

    private  String JobCategory;
    private String jobId;
    private String jobTitle;

    private String companyName;
    private String city;
    private String salary;
    private String jobDescription;
    private String workType;
    private String designation;
    private String jobResponsibilities;
    private List<String> requiredSkills;
    private String companyImageUrl;
    private String employerId;
    private long timestamp;
    private boolean isActive;



    // ... existing fields ...
    private String ageRequirement;
    private String genderPreference;
    // Default constructor required for Firebase
    public Job() {
    }

    public Job(String jobId, String jobTitle, String companyName, String city, String salary,
               String jobDescription, String workType, String designation, String jobResponsibilities,
               List<String> requiredSkills, String companyImageUrl, String employerId,
               long timestamp, boolean isActive, String ageRequirement, String genderPreference) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.city = city;
        this.salary = salary;
        this.jobDescription = jobDescription;
        this.workType = workType;
        this.designation = designation;
        this.jobResponsibilities = jobResponsibilities;
        this.requiredSkills = requiredSkills;
        this.companyImageUrl = companyImageUrl;
        this.employerId = employerId;
        this.timestamp = timestamp;
        this.isActive = isActive;
        this.ageRequirement = ageRequirement;
        this.genderPreference = genderPreference;
    }

    // Getters and setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getJobResponsibilities() { return jobResponsibilities; }
    public void setJobResponsibilities(String jobResponsibilities) { this.jobResponsibilities = jobResponsibilities; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public String getCompanyImageUrl() { return companyImageUrl; }
    public void setCompanyImageUrl(String companyImageUrl) { this.companyImageUrl = companyImageUrl; }

    public String getEmployerId() { return employerId; }
    public void setEmployerId(String employerId) { this.employerId = employerId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Add getters and setters
    public String getAgeRequirement() { return ageRequirement; }
    public void setAgeRequirement(String ageRequirement) { this.ageRequirement = ageRequirement; }

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }
}