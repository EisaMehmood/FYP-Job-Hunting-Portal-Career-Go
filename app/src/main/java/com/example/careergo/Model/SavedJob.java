package com.example.careergo.Model;

public class SavedJob {
    private String savedJobId;
    private String jobId;
    private String studentId;
    private String studentId_jobId;
    private long savedDate;

    // Add these fields to store job details
    private Job jobDetails;

    public SavedJob() {
        // Default constructor required for Firebase
    }

    public SavedJob(String savedJobId, String jobId, String studentId, String studentId_jobId, long savedDate) {
        this.savedJobId = savedJobId;
        this.jobId = jobId;
        this.studentId = studentId;
        this.studentId_jobId = studentId_jobId;
        this.savedDate = savedDate;
    }

    // Getters and Setters
    public String getSavedJobId() { return savedJobId; }
    public void setSavedJobId(String savedJobId) { this.savedJobId = savedJobId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentId_jobId() { return studentId_jobId; }
    public void setStudentId_jobId(String studentId_jobId) { this.studentId_jobId = studentId_jobId; }

    public long getSavedDate() { return savedDate; }
    public void setSavedDate(long savedDate) { this.savedDate = savedDate; }

    public Job getJobDetails() { return jobDetails; }
    public void setJobDetails(Job jobDetails) { this.jobDetails = jobDetails; }
}