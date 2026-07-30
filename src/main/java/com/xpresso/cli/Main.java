package com.xpresso.cli;

import java.io.File;
import java.util.Scanner;

import com.xpresso.analyzer.Analyzer;

/**
 * CLI entry point for X-presso.
 */
public class Main {
    public static void main(String[] args) {
        int code = 0;
        try {
            CliOptions options = args.length == 0
                ? promptInteractive()
                : CliOptions.parse(args);

            if (!isReadableFile(options.filePath)) {
                System.err.println("Cannot read file: " + options.filePath);
                System.exit(1);
            }

            System.out.println("X-presso - analyzing " + options.filePath);
            Analyzer.Mode mode = options.lexOnly ? Analyzer.Mode.LEX : Analyzer.Mode.PARSE;
            Analyzer.Result result = Analyzer.analyzeFile(options.filePath, mode);
            TokenReporter.report(result, options);
            if (!result.ok) {
                code = 1;
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            code = 1;
        } catch (Exception e) {
            System.err.println("Fatal: " + e.getMessage());
            e.printStackTrace();
            code = 1;
        }
        System.exit(code);
    }

    private static CliOptions promptInteractive() {
        Scanner sc = new Scanner(System.in);
        System.out.println("X-presso (interactive)");
        System.out.print("Source file: ");
        String path = sc.nextLine().trim();
        System.out.print("Lex only? (yes/no): ");
        boolean lexOnly = sc.nextLine().trim().equalsIgnoreCase("yes");
        System.out.print("Verbose tokens? (yes/no): ");
        boolean verbose = sc.nextLine().trim().equalsIgnoreCase("yes");
        System.out.print("Output format (text/json): ");
        String fmt = sc.nextLine().trim();
        if (!fmt.equals("json")) fmt = "text";
        System.out.print("Write to output/? (yes/no): ");
        boolean file = sc.nextLine().trim().equalsIgnoreCase("yes");
        return new CliOptions(path, verbose, file, lexOnly, fmt);
    }

    private static boolean isReadableFile(String path) {
        if (path == null || path.isBlank()) return false;
        File f = new File(path);
        return f.isFile() && f.canRead();
    }
}
