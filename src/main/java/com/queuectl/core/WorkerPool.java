package com.queuectl.core;

import com.queuectl.storage.JobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class WorkerPool {

    private final JobRepository repository;
    private final int workerCount;
    private final int baseBackoff;
    private final ExecutorService executorService;
    private final List<Worker> workers = new ArrayList<>();

    public WorkerPool(JobRepository repository, int workerCount, int baseBackoff) {
        this.repository = repository;
        this.workerCount = workerCount;
        this.baseBackoff = baseBackoff;
        this.executorService = Executors.newFixedThreadPool(workerCount);
    }


    public void start() {
        System.out.println("🚀 Starting " + workerCount + " worker(s)...");
        for (int i = 1; i <= workerCount; i++) {
            Worker worker = new Worker("worker-" + i, repository, baseBackoff);
            workers.add(worker);
            executorService.submit(worker);
        }
    }

    public void stop() {
        System.out.println("Stopping all workers...");
        workers.forEach(Worker::stop);
        executorService.shutdownNow();
        System.out.println("All workers stopped.");
    }


    public int getActiveWorkerCount() {
        return workerCount;
    }
}
