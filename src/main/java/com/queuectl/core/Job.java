package com.queuectl.core;

import java.time.Instant;
import java.util.Objects;

public class Job {
    private String id;
    private String command;
    private JobState state;
    private int attempts;
    private int maxRetries;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant nextRunAt;
    private String lastError;
    private String output;

    public Job(String id, String command, int maxRetries) {
        this.id = id;
        this.command = command;
        this.state = JobState.PENDING;
        this.attempts = 0;
        this.maxRetries = maxRetries;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // getters & setters

    public String getId() { return id; }
    public String getCommand() { return command; }
    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; setUpdatedAt(Instant.now()); }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; setUpdatedAt(Instant.now()); }
    public int getMaxRetries() { return maxRetries; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; setUpdatedAt(Instant.now()); }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; setUpdatedAt(Instant.now()); }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; setUpdatedAt(Instant.now()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;
        return Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
