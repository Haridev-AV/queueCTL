package com.queuectl.storage;

import com.queuectl.core.Job;
import com.queuectl.core.JobState;

import java.util.List;
import java.util.Optional;

public interface JobRepository {
    void init() throws Exception;
    void save(Job job) throws Exception;
    Optional<Job> findById(String id) throws Exception;
    List<Job> listByState(JobState state) throws Exception;
    void updateJobState(String jobId, JobState state);
    void updateJobOutput(String jobId, String output);
    Optional<Job> fetchNextPendingJob();
    JobState getJobState(String jobId);
    void updateJobAttempts(String jobId, int attempts);
    void moveToDLQ(Job job);


}
