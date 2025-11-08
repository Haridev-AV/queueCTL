package com.queuectl.core;

import com.queuectl.storage.JobRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JobExecutor {

    private final JobRepository repository;

    public JobExecutor(JobRepository repository) {
        this.repository = repository;
    }

    public void execute(Job job) {
        try {
            repository.updateJobState(job.getId(), JobState.PROCESSING);
            Process process = null;
            StringBuilder output = new StringBuilder();

            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder builder;

                if (os.contains("win")) {
                    builder = new ProcessBuilder("cmd.exe", "/c", job.getCommand());
                } else {
                    builder = new ProcessBuilder("bash", "-c", job.getCommand());
                }

                builder.redirectErrorStream(true);
                process = builder.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                }

                int exitCode = process.waitFor();
                repository.updateJobOutput(job.getId(), output.toString());

                if (exitCode == 0) {
                    repository.updateJobState(job.getId(), JobState.COMPLETED);
                    System.out.println("Job " + job.getId() + " completed successfully");
                } else {
                    repository.updateJobState(job.getId(), JobState.FAILED);
                    System.err.println("Job " + job.getId() + " failed with exit code " + exitCode);
                }

            } catch (IOException | InterruptedException e) {
                repository.updateJobState(job.getId(), JobState.FAILED);
                repository.updateJobOutput(job.getId(), e.getMessage());
                System.err.println("Job " + job.getId() + " failed due to: " + e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                if (process != null) process.destroy();
            }
        } catch (Exception e) {
            System.err.println("Repository error for job " + job.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
