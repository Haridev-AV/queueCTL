package com.queuectl.cli;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.queuectl.core.ConfigManager;
import com.queuectl.core.Job;
import com.queuectl.storage.JobRepository;
import com.queuectl.storage.SQLiteStorage;

import java.util.UUID;

public class CommandHandler {
    private final JobRepository repo;

    public CommandHandler() throws Exception {
        this.repo = new SQLiteStorage();
        this.repo.init();
    }

    public void handleEnqueue(String jobJson) throws Exception {
        // Accept either full job JSON (with id, command, max_retries) or minimal (command)
        Gson g = new Gson();
        JsonObject obj = g.fromJson(jobJson, JsonObject.class);
        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
        String command = obj.has("command") ? obj.get("command").getAsString() : null;
        if (command == null) {
            System.err.println("enqueue requires a 'command' field");
            return;
        }
        int maxRetries = obj.has("max_retries") ? obj.get("max_retries").getAsInt()
                : ConfigManager.getInstance().getInt("max_retries", 3);

        Job job = new Job(id, command, maxRetries);
        repo.save(job);
        System.out.printf("Enqueued job id=%s command=\"%s\"%n", id, command);
    }
}
