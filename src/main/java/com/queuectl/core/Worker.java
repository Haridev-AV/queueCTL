package com.queuectl.core;

import com.queuectl.storage.JobRepository;
import java.util.Optional;

public class Worker implements Runnable {

    private final String workerId;
    private final JobRepository repository;
    private final JobExecutor executor;
    private final int baseBackoff;
    private volatile boolean running = true;

    public Worker(String workerId, JobRepository repository, int baseBackoff) {
        this.workerId = workerId;
        this.repository = repository;
        this.executor = new JobExecutor(repository);
        this.baseBackoff = baseBackoff;
    }

    @Override
    public void run() {
        System.out.println("Worker " + workerId + " started.");

        while (running) {
            try {
                Optional<Job> optionalJob = repository.fetchNextPendingJob();

                if (optionalJob.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }

                Job job = optionalJob.get();
                System.out.println("Worker " + workerId + " processing job: " + job.getId());
                repository.updateJobState(job.getId(), JobState.PROCESSING);

                executor.execute(job);

                if (repository.getJobState(job.getId()) == JobState.FAILED) {
                    handleRetry(job);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Worker " + workerId + " error: " + e.getMessage());
            }
        }

        System.out.println("Worker " + workerId + " stopped gracefully.");
    }

    private void handleRetry(Job job) throws InterruptedException {
        int attempts = job.getAttempts() + 1;
        job.setAttempts(attempts);
        repository.updateJobAttempts(job.getId(), attempts);

        if (attempts > job.getMaxRetries()) {
            repository.updateJobState(job.getId(), JobState.DEAD);
            repository.moveToDLQ(job);
            System.err.println("Job " + job.getId() + " moved to Dead Letter Queue");
        } else {
            int delay = (int) Math.pow(baseBackoff, attempts);
            System.out.println("Retrying job " + job.getId() + " after " + delay + "s");
            repository.updateJobState(job.getId(), JobState.PENDING);
            Thread.sleep(delay * 1000L);
        }
    }

    public void stop() {
        running = false;
    }
}
