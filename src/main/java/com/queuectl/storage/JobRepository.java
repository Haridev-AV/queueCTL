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
    void updateJobState(String jobId, JobState state) throws Exception;
    void updateJobOutput(String jobId, String output) throws Exception;
    JobState getJobState(String jobId) throws Exception;
    void updateJobAttempts(String jobId, int attempts) throws Exception;
    void moveToDLQ(Job job) throws Exception;
    Optional<Job> fetchNextPendingJob() throws Exception;


}
