package org.example.config;

import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.example.infer.InferUtils.packageName;
import static org.example.infer.InferUtils.wrapperClassName;

public class InferConfig {
    @SerializedName("pulse-taint-sources")
    List<TaintConfig> sources;

    @SerializedName("pulse-taint-sinks")
    List<TaintConfig> sinks;

    public InferConfig() {
        TaintConfig source = new TaintConfig(
            List.of("%s.%s".formatted(packageName, wrapperClassName)),
            List.of("left")
        );

        sources = List.of(source);

        TaintConfig sink = new TaintConfig(
            List.of("%s.%s".formatted(packageName, wrapperClassName)),
            List.of("right")
        );
        sinks = List.of(sink);
    }

    public void swap() {
        List<TaintConfig> temp = new ArrayList<>(sources);
        sources = new ArrayList<>(sinks);
        sinks = new ArrayList<>(temp);
    }


    public void execInfer(String command) {

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.directory(new File("/home/rique/Documents/research/infer-data-flow-test"));

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
}

