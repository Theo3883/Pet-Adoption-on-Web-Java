package com.backend.controller.taskstatus;

import java.util.concurrent.Future;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TaskStatus<T> {
    private final Future<T> future;
    private final String filename;
    private final String mediaType;
    private final long startTimestamp;
    @Setter
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

    public boolean isDone() { return future.isDone(); }
    public boolean isCancelled() { return future.isCancelled(); }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTimestamp;
    }
}
