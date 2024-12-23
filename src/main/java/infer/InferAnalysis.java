package infer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static infer.InferUtils.*;

public class InferAnalysis {
    private InferConfig inferConfig;

    public InferAnalysis(InferConfig inferConfig) {
        this.inferConfig = inferConfig;
    }

    public void executeDataFlowAnalysis(String projectPath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(inferConfig);

        Path inferPackagePath = Path.of(projectPath, INFER_PACKAGE_PATH);
        String inferConfigUri = inferPackagePath.resolve(INFER_CONFIG_NAME).toString();
        String inferOutUri = inferPackagePath.resolve(INFER_OUT).toString();
        String gradlewPath = Path.of(projectPath, "gradlew").toString();

        String buildCommand;
        if (isGradleProject(projectPath)) {
            buildCommand = String.format("%s clean build", gradlewPath);
        } else if (isMavenProject(projectPath)) {
            buildCommand = String.format("mvn clean install");
        } else {
            throw new IllegalStateException("Unsupported project type. Neither Gradle nor Maven build files were found.");
        }

        try (FileWriter writer = new FileWriter(inferConfigUri)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String captureCommand = String.format("infer capture -o %s -- %s", inferOutUri, buildCommand);
        String analyzeCommand = String.format("infer analyze --pulse-only --pulse-taint-config %s -o %s", inferConfigUri, inferOutUri);

        System.out.println(captureCommand);
        System.out.println("Capture Infer Phase...");
        execInfer(captureCommand, projectPath);

        System.out.println("Infer Executing left -> right");
        System.out.println(analyzeCommand);
        execInfer(analyzeCommand, projectPath);

        renameReportFile(inferOutUri, "left-to-right-report.txt");

        inferConfig.swap();
        json = gson.toJson(inferConfig);

        try (FileWriter writer = new FileWriter(Path.of(projectPath, INFER_PACKAGE_PATH, INFER_CONFIG_NAME).toString())) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Infer Executing right -> left...");
        System.out.println(analyzeCommand);
        execInfer(analyzeCommand, projectPath);

        renameReportFile(inferOutUri, "right-to-left-report.txt");
    }

    private void renameReportFile(String inferOutUri, String newFileName) {
        Path reportFile = Path.of(inferOutUri, "report.txt");
        Path renamedReportFile = Path.of(inferOutUri, newFileName);

        try {
            if (Files.exists(reportFile)) {
                Files.move(reportFile, renamedReportFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error renaming report file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isGradleProject(String projectPath) {
        Path gradleFile = Path.of(projectPath, "build.gradle");
        Path gradleKtsFile = Path.of(projectPath, "build.gradle.kts");
        Path gradlewFile = Path.of(projectPath, "gradlew");
        return Files.exists(gradleFile) || Files.exists(gradleKtsFile) || Files.exists(gradlewFile);
    }

    private boolean isMavenProject(String projectPath) {
        Path mavenFile = Path.of(projectPath, "pom.xml");
        return Files.exists(mavenFile);
    }


    private void execInfer(String command, String pathExecution) {

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
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
}
