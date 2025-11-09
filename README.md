# QueueCTL - Lightweight Job Queue System

[Watch the video demo](https://drive.google.com/drive/folders/1GwXbVHfRCVN14URDeGmZEiu5ogTpsFqq?usp=drive_link)

A simple persistent job queue implementation in Java that processes shell commands with retry logic and dead letter queue support.

## Features

- CLI-based job management
- SQLite persistence
- Automatic retry with exponential backoff
- Dead Letter Queue for failed jobs
- Configurable worker threads

## Installation

### Clone & Build
```bash
git clone https://github.com/Haridev-AV/queueCTL.git
cd queueCTL
mvn clean package
```

### Setup Global Command (Optional)

**Windows:**
Create `queuectl.bat` in project directory:
```bat
@echo off
java -jar "C:\path\to\queuectl-0.1.0.jar" %*
```
Add to PATH.

**Linux/macOS:**
```bash
#!/bin/bash
java -jar /path/to/queuectl-0.1.0.jar "$@"
```
Save as `/usr/local/bin/queuectl` and run `chmod +x /usr/local/bin/queuectl`

## Usage

### Basic Commands

| Command | Description | Example |
|---------|-------------|---------|
| `-e, --enqueue` | Add job to queue | `queuectl -e "{\"command\":\"echo Hello\"}"` |
| `-w, --start-workers` | Start worker threads | `queuectl -w 3` |
| `-x, --stop-workers` | Stop all workers | `queuectl -x` |
| `-l, --list` | List jobs by state | `queuectl -l PENDING` |
| `-s, --status` | Show job summary | `queuectl -s` |
| `-d, --dlq-list` | View dead letter queue | `queuectl -d` |
| `-r, --dlq-retry` | Retry failed job | `queuectl -r <job-id>` |
| `-c, --config-set` | Update config | `queuectl -c max_retries=5` |

### Job States

| State | Description |
|-------|-------------|
| PENDING | Waiting for worker |
| PROCESSING | Currently executing |
| COMPLETED | Successfully finished |
| FAILED | Failed but retryable |
| DEAD | Moved to DLQ |

## How It Works

1. **Enqueue**: Jobs stored in SQLite as PENDING
2. **Workers**: Pick and execute pending jobs
3. **Retry**: Failed jobs retry with exponential backoff (2s, 4s, 8s...)
4. **DLQ**: Jobs exceeding max retries move to dead letter queue

## Configuration

Adjust retry settings:
```bash
queuectl --config-set max_retries 5
queuectl --config-set base_backoff 2
```

## Tech Stack

- Java 17+
- SQLite (JDBC)
- Apache Commons CLI
- Gson
- Maven

## Example Workflow
```bash
# Start workers
queuectl -w 2

# Add some jobs
queuectl -e "{\"command\":\"echo Task 1\"}"
queuectl -e "{\"command\":\"echo Task 2\"}"
queuectl --enqueue "{\"command\":\"cmd /c exit 1\"}"

# Check status
queuectl -s

# View dead letters
queuectl -d
```
