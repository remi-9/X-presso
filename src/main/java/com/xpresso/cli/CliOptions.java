package com.xpresso.cli;

/**
 * Parsed command-line options for the X-presso CLI.
 */
public final class CliOptions {
    public final String filePath;
    public final boolean verbose;
    public final boolean outputToFile;
    public final boolean lexOnly;
    public final String outputFormat;

    public CliOptions(String filePath, boolean verbose, boolean outputToFile,
                      boolean lexOnly, String outputFormat) {
        this.filePath = filePath;
        this.verbose = verbose;
        this.outputToFile = outputToFile;
        this.lexOnly = lexOnly;
        this.outputFormat = outputFormat;
    }

    public static CliOptions parse(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                "Usage: Main <file> [--lex-only] [--verbose] [--file] [--output=text|json]"
            );
        }
        String filePath = args[0];
        boolean verbose = false;
        boolean outputToFile = false;
        boolean lexOnly = false;
        String outputFormat = "text";

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "--verbose" -> verbose = true;
                case "--file" -> outputToFile = true;
                case "--lex-only" -> lexOnly = true;
                default -> {
                    if (arg.startsWith("--output=")) {
                        String fmt = arg.substring("--output=".length());
                        if (!fmt.equals("text") && !fmt.equals("json")) {
                            throw new IllegalArgumentException("output must be text or json");
                        }
                        outputFormat = fmt;
                    }
                }
            }
        }
        return new CliOptions(filePath, verbose, outputToFile, lexOnly, outputFormat);
    }
}
