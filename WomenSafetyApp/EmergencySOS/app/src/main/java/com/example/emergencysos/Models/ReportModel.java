package com.example.emergencysos.Models;

public class ReportModel {
    private String location;
    private String datetime;
    private String type;
    private String filePath;

    public ReportModel() {}

    public ReportModel(String location, String datetime, String type, String filePath) {
        this.location = location;
        this.datetime = datetime;
        this.type = type;
        this.filePath = filePath;
    }

    public String getLocation() {
        return location;
    }

    public String getDatetime() {
        return datetime;
    }

    public String getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
