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
        return DriverManager.getConnection(dbUrl);
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
}
