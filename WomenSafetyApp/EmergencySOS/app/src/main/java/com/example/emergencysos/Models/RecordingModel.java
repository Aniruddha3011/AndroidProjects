package com.example.emergencysos.Models;

public class RecordingModel {
    public String id, fileName, downloadUrl, timestamp, duration;

    public RecordingModel() {}

    public RecordingModel(String id, String fileName, String downloadUrl, long timestamp, String duration) {
        this.id = id;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.timestamp = String.valueOf(timestamp);
        this.duration = duration;
    }
}

