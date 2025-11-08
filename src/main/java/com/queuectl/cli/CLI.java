package com.queuectl.cli;

import org.apache.commons.cli.*;

public class CLI {
    public static void main(String[] args) {
        Options opts = new Options();

        // --- Define commands ---
        Option enqueue = new Option("e", "enqueue", true, "Enqueue job with JSON payload");
        enqueue.setArgs(1);
        enqueue.setOptionalArg(false);
        opts.addOption(enqueue);

        Option startWorkers = new Option("w", "start-workers", true, "Start worker pool with N workers");
        startWorkers.setArgs(1);
        opts.addOption(startWorkers);

        Option stopWorkers = new Option("x", "stop-workers", false, "Stop all running workers");
        opts.addOption(stopWorkers);

        Option status = new Option("s", "status", false, "Show job status summary");
        opts.addOption(status);

        Option list = new Option("l", "list", true, "List jobs by state (PENDING, PROCESSING, COMPLETED, FAILED, DEAD)");
        list.setArgs(1);
        opts.addOption(list);

        Option dlqList = new Option("d", "dlq-list", false, "List jobs in Dead Letter Queue");
        opts.addOption(dlqList);

        Option dlqRetry = new Option("r", "dlq-retry", true, "Retry a job from DLQ by ID");
        dlqRetry.setArgs(1);
        opts.addOption(dlqRetry);

        Option configSet = new Option("c", "config-set", true, "Set config key=value");
        configSet.setArgs(2);
        opts.addOption(configSet);

        Option help = new Option("h", "help", false, "Print help");
        opts.addOption(help);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption("help") || args.length == 0) {
                formatter.printHelp("queuectl", opts);
                return;
            }

            CommandHandler handler = new CommandHandler();

            if (cmd.hasOption("enqueue")) {
                String payload = cmd.getOptionValue("enqueue");
                handler.handleEnqueue(payload);
                return;
            }

            if (cmd.hasOption("start-workers")) {
                int count = Integer.parseInt(cmd.getOptionValue("start-workers", "1"));
                handler.handleWorkerStart(count);
                return;
            }

            if (cmd.hasOption("stop-workers")) {
                System.out.println("Stop workers functionality not implemented yet.");
                return;
            }

            if (cmd.hasOption("status")) {
                handler.handleStatus();
                return;
            }

            if (cmd.hasOption("list")) {
                String state = cmd.getOptionValue("list");
                handler.handleList(state);
                return;
            }

            if (cmd.hasOption("dlq-list")) {
                handler.handleDLQList();
                return;
            }

            if (cmd.hasOption("dlq-retry")) {
                String jobId = cmd.getOptionValue("dlq-retry");
                handler.handleDLQRetry(jobId);
                return;
            }

            if (cmd.hasOption("config-set")) {
                String[] values = cmd.getOptionValues("config-set");
                if (values.length < 2) {
                    System.err.println("config-set requires key and value");
                    return;
                }
                handler.handleConfigSet(values[0], values[1]);
                return;
            }

            System.err.println("Unknown command");
            formatter.printHelp("queuectl", opts);

        } catch (ParseException pe) {
            System.err.println("Failed to parse CLI args: " + pe.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}