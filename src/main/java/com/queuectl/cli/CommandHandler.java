package com.queuectl.cli;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.queuectl.core.ConfigManager;
import com.queuectl.core.Job;
import com.queuectl.core.JobState;
import com.queuectl.storage.JobRepository;
import com.queuectl.storage.SQLiteStorage;
import com.queuectl.core.WorkerPool;
import java.util.*;

public class CommandHandler {
    private final JobRepository repo;

    public CommandHandler() throws Exception {
        this.repo = new SQLiteStorage();
        this.repo.init();
    }

    public void handleEnqueue(String jobJson) throws Exception {
        Gson g = new Gson();
        JsonObject obj = g.fromJson(jobJson, JsonObject.class);

        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
        String command = obj.has("command") ? obj.get("command").getAsString() : null;

        if (command == null || command.isBlank()) {
            System.err.println("enqueue requires a 'command' field");
            return;
        }

        int maxRetries = obj.has("max_retries")
                ? obj.get("max_retries").getAsInt()
                : ConfigManager.getInstance().getInt("max_retries", 3);

        Job job = new Job(id, command, maxRetries);
        repo.save(job);
        System.out.printf("Enqueued job id=%s command=\"%s\"%n", id, command);
    }

    public void handleList(String stateName) throws Exception {
        JobState state = JobState.valueOf(stateName.toUpperCase());
        List<Job> jobs = repo.listByState(state);

        if (jobs.isEmpty()) {
            System.out.println("No jobs found in state: " + state);
            return;
        }

        System.out.printf("Jobs in state: %s%n", state);
        System.out.println("--------------------------------------------------");
        for (Job job : jobs) {
            System.out.printf("ID: %-10s | CMD: %-20s | Attempts: %d/%d%n",
                    job.getId(), job.getCommand(), job.getAttempts(), job.getMaxRetries());
        }
    }

    public void handleStatus() throws Exception {
        Map<JobState, Long> counts = new EnumMap<>(JobState.class);
        for (JobState s : JobState.values()) {
            counts.put(s, (long) repo.listByState(s).size());
        }

        System.out.println("Job Status Summary:");
        System.out.println("-------------------------");
        for (var entry : counts.entrySet()) {
            System.out.printf("%-10s : %d%n", entry.getKey(), entry.getValue());
        }
    }

    public void handleWorkerStart(int count) throws Exception {
        int baseBackoff = ConfigManager.getInstance().getInt("base_backoff", 2);
        WorkerPool pool = new WorkerPool(repo, count, baseBackoff);
        System.out.printf("Starting %d worker(s)...%n", count);
        pool.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down workers gracefully...");
            pool.stop();
        }));

        // Keep main thread alive
        while (true) {
            Thread.sleep(2000);
        }
    }

    public void handleDLQList() throws Exception {
    List<Job> dlqJobs = repo.listDLQ();  // CHANGED THIS LINE
    if (dlqJobs.isEmpty()) {
        System.out.println("No jobs in Dead Letter Queue.");
        return;
    }

    System.out.println("Dead Letter Queue Jobs:");
    System.out.println("--------------------------------------------------");
    for (Job job : dlqJobs) {
        System.out.printf("ID: %-36s | CMD: %-30s%n",
                job.getId(), job.getCommand());
    }
}
    public void handleDLQRetry(String jobId) throws Exception {
    Optional<Job> jobOpt = repo.findInDLQ(jobId); // CHANGED
    if (jobOpt.isEmpty()) {
        System.err.println("Job not found in DLQ: " + jobId);
        return;
    }

    Job job = jobOpt.get();
    
    // Remove from DLQ
    repo.deleteFromDLQ(job.getId()); // CHANGED

    // Reset for retry
    job.setState(JobState.PENDING);
    job.setAttempts(0);
    job.setLastError(null);

    repo.save(job);

    System.out.printf("Moved job %s back to queue for retry.%n", job.getId());
}



    public void handleConfigSet(String key, String value) throws Exception {
        ConfigManager cfg = ConfigManager.getInstance();
        cfg.set(key, value);
        System.out.printf("Updated config: %s = %s%n", key, value);
    }

    private WorkerPool workerPool;

    public void startWorkers(int count) throws Exception {
        if (workerPool == null) {
            int baseBackoff = ConfigManager.getInstance().getInt("base_backoff_ms", 1000);
            workerPool = new WorkerPool(repo, count, baseBackoff);
            workerPool.start();
        } else {
            System.out.println("Workers already running!");
        }
    }

    public void stopWorkers() {
        if (workerPool != null) {
            workerPool.stop();
            workerPool = null;
        } else {
            System.out.println("No workers are running.");
        }
    }

    public void showStatus() {
        if (workerPool != null) {
            System.out.println("Active workers: " + workerPool.getActiveWorkerCount());
        } else {
            System.out.println("No workers are currently running.");
        }
    }

}