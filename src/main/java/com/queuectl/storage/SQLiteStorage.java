package com.queuectl.storage;

import com.queuectl.core.Job;
import com.queuectl.core.JobState;
import com.queuectl.core.ConfigManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteStorage implements JobRepository {
    private final String dbUrl;

    public SQLiteStorage() {
        String path = ConfigManager.getInstance().get("database_path", "queuectl.db");
        this.dbUrl = "jdbc:sqlite:" + path;
    }

    private Connection conn() throws SQLException {
        Connection c = DriverManager.getConnection(dbUrl);
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA busy_timeout=5000;");
        }
        return c;
    }

    @Override
    public void init() throws Exception {
        String schema;
        try (var in = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) throw new RuntimeException("schema.sql not found");
            schema = new String(in.readAllBytes());
        }
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA journal_mode=WAL;");
            st.executeUpdate("PRAGMA busy_timeout=5000;");
            st.executeUpdate("PRAGMA synchronous=NORMAL;");
            for (String stmt : schema.split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) st.executeUpdate(s + ";");
            }
        }
    }

    @Override
    public void save(Job job) throws Exception {
        String sql = "INSERT INTO jobs (id, command, state, attempts, max_retries, created_at, updated_at, next_run_at, last_error, output) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, job.getId());
            ps.setString(2, job.getCommand());
            ps.setString(3, job.getState().name());
            ps.setInt(4, job.getAttempts());
            ps.setInt(5, job.getMaxRetries());
            ps.setString(6, job.getCreatedAt().toString());
            ps.setString(7, job.getUpdatedAt().toString());
            ps.setString(8, job.getNextRunAt() == null ? null : job.getNextRunAt().toString());
            ps.setString(9, job.getLastError());
            ps.setString(10, job.getOutput());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Job> findById(String id) throws Exception {
        String sql = "SELECT * FROM jobs WHERE id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            var rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();
            return Optional.of(rowToJob(rs));
        }
    }

    @Override
    public List<Job> listByState(JobState state) throws Exception {
        String sql = "SELECT * FROM jobs WHERE state = ?";
        List<Job> out = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, state.name());
            var rs = ps.executeQuery();
            while (rs.next()) out.add(rowToJob(rs));
        }
        return out;
    }
    
    private Job rowToJob(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String cmd = rs.getString("command");
        int maxRetries = rs.getInt("max_retries");
        Job j = new Job(id, cmd, maxRetries);
        j.setAttempts(rs.getInt("attempts"));
        j.setState(JobState.valueOf(rs.getString("state")));
        // created/updated timestamps - naive parsing
        try { j.setUpdatedAt(Instant.parse(rs.getString("updated_at"))); } catch (Exception ignored) {}
        try { /* leave createdAt from constructor */ } catch (Exception ignored) {}
        return j;
    }

    @Override
    public void updateJobState(String id, JobState state) throws Exception {
        String sql = "UPDATE jobs SET state = ?, updated_at = ? WHERE id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, state.name());
            ps.setString(2, Instant.now().toString());
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateJobOutput(String id, String output) throws Exception {
        String sql = "UPDATE jobs SET output = ?, updated_at = ? WHERE id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, output);
            ps.setString(2, Instant.now().toString());
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    @Override
    public JobState getJobState(String id) throws Exception {
        String sql = "SELECT state FROM jobs WHERE id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return JobState.valueOf(rs.getString("state"));
            } else {
                throw new RuntimeException("Job not found: " + id);
            }
        }
    }

    @Override
    public void updateJobAttempts(String id, int attempts) throws Exception {
        String sql = "UPDATE jobs SET attempts = ?, updated_at = ? WHERE id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setString(2, Instant.now().toString());
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Job> fetchNextPendingJob() throws Exception {
        String selectSql = "SELECT * FROM jobs WHERE state = 'PENDING' ORDER BY created_at LIMIT 1";
        String updateSql = "UPDATE jobs SET state = 'RUNNING', updated_at = ? WHERE id = ? AND state = 'PENDING'";
        
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (Connection c = conn()) {
                c.setAutoCommit(false);
                
                try (PreparedStatement selectPs = c.prepareStatement(selectSql)) {
                    var rs = selectPs.executeQuery();
                    if (!rs.next()) {
                        c.rollback();
                        return Optional.empty();
                    }
                    
                    String jobId = rs.getString("id");
                    
                    try (PreparedStatement updatePs = c.prepareStatement(updateSql)) {
                        updatePs.setString(1, Instant.now().toString());
                        updatePs.setString(2, jobId);
                        int updated = updatePs.executeUpdate();
                        
                        if (updated == 0) {
                            // Another worker claimed it first
                            c.rollback();
                            return Optional.empty();
                        }
                        
                        c.commit();
                        return Optional.of(rowToJob(rs));
                    }
                } catch (SQLException e) {
                    c.rollback();
                    if (e.getMessage().contains("locked") && attempt < maxRetries - 1) {
                        Thread.sleep(100 * (attempt + 1));
                        continue;
                    }
                    throw e;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void moveToDLQ(Job job) throws Exception {
        String reason = job.getLastError() != null ? job.getLastError() : "Exceeded max retries";

        try (Connection c = conn();
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO dlq (id, command, reason, failed_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, job.getId());
            ps.setString(2, job.getCommand());
            ps.setString(3, reason);
            ps.setString(4, Instant.now().toString());
            ps.executeUpdate();
        }

        // Remove from main jobs table
        try (Connection c = conn();
            PreparedStatement ps = c.prepareStatement("DELETE FROM jobs WHERE id = ?")) {
            ps.setString(1, job.getId());
            ps.executeUpdate();
        }
    }

}