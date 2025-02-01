package org.example.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Terminal {
    public static void executeCommand(String pathExecution, List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pathExecution));

        try {
            Process process = processBuilder.start();

            // Capture the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Process finished with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void executeCommand(String pathExecution, String command) {
        executeCommand(pathExecution, List.of(command.split(" ")));
    }
}
