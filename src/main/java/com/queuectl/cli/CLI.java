package com.queuectl.cli;

import org.apache.commons.cli.*;

public class CLI {
    public static void main(String[] args) {
        Options opts = new Options();
        Option enqueue = new Option("e", "enqueue", true, "Enqueue job with JSON payload");
        enqueue.setArgs(1);
        enqueue.setOptionalArg(false);
        opts.addOption(enqueue);

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
