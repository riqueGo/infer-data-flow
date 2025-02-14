package org.example.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Terminal {
    public static List<String> executeCommandAndGetOutput(String pathExecution, List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pathExecution));
        List<String> output = new ArrayList<>();

        try {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Process finished with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static void executeCommand(String pathExecution, List<String> command) throws RuntimeException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pathExecution));
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();

            // Capture the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Command execution failed with exit code " + exitCode + "\n");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void executeCommand(String pathExecution, String command) throws RuntimeException {
        executeCommand(pathExecution, List.of(command.split(" ")));
    }
}
