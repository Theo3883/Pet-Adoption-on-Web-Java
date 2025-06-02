package com.backend.controller.taskstatus;

import java.util.concurrent.Future;

public class TaskStatus<T> {
    private final Future<T> future;
    private final String filename;
    private final String mediaType;
    private final long startTimestamp;
    private volatile String status;
    private final String taskType; // "upload", "download", "delete"

    public TaskStatus(Future<T> future, String filename, String mediaType, String taskType) {
        this.future = future;
        this.filename = filename;
        this.mediaType = mediaType;
        this.taskType = taskType;
        this.startTimestamp = System.currentTimeMillis();
        this.status = "processing";
    }

    // Getters
    public Future<T> getFuture() { return future; }
    public String getFilename() { return filename; }
    public String getMediaType() { return mediaType; }
    public String getTaskType() { return taskType; }
    public long getStartTimestamp() { return startTimestamp; }
    public String getStatus() { return status; }
    
    // Setters
    public void setStatus(String status) { this.status = status; }

    // Utility methods
    public boolean isDone() { return future.isDone(); }
    public boolean isCancelled() { return future.isCancelled(); }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTimestamp;
    }
}
