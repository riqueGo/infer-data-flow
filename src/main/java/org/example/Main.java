package org.example;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.example.config.Arguments;
import org.example.config.CliConfig;

public class Main {
    public static void main(String[] args) {
        CliConfig cli = new CliConfig();
        try {
            Arguments result = cli.parseCommandLine(args);
            new StaticAnalysisMerge(result).run();
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            new HelpFormatter().printHelp("java Main", cli.getOptions());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}