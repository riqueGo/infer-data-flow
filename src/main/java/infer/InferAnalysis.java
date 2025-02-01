package infer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static infer.InferConstants.*;
import static org.example.utils.PathToString.getPath;
import static org.example.utils.PathToString.resolvePath;
import static org.example.utils.Terminal.executeCommand;

public class InferAnalysis {

    public void executeDataFlowAnalysis(String projectPath) {
        Path inferDestinationPackagePath = Path.of(projectPath, INFER_PACKAGE_PATH);
        Path inferSourcePackagePath = Path.of(WORKING_DIRECTORY, INFER_PACKAGE_PATH);
        String inferOutUri = resolvePath(inferDestinationPackagePath, INFER_OUT);
        String gradlewPath = getPath(projectPath, "gradlew");

        String buildCommand;
        if (isGradleProject(projectPath)) {
            buildCommand = String.format("%s clean build", gradlewPath);
        } else if (isMavenProject(projectPath)) {
            buildCommand = String.format("mvn clean install");
        } else {
            throw new IllegalStateException("Unsupported project type. Neither Gradle nor Maven build files were found.");
        }

        String captureCommand = String.format("infer capture -o %s -- %s", inferOutUri, buildCommand);
        String analyzeCommand = String.format("infer analyze --pulse-only -o %s", inferOutUri);
        String rightToLeftAnalysis = String.format("%s --inferconfig-path %s", analyzeCommand, inferSourcePackagePath.resolve(INFER_CONFIG_RIGHT_TO_LEFT));
        String leftToRightAnalysis = String.format("%s --inferconfig-path %s", analyzeCommand, inferSourcePackagePath.resolve(INFER_CONFIG_LEFT_TO_RIGHT));

        System.out.println(captureCommand);
        System.out.println("Capture Infer Phase...");
        executeCommand(projectPath, captureCommand);

        System.out.println("Infer Executing left -> right");
        System.out.println(leftToRightAnalysis);
        executeCommand(projectPath, leftToRightAnalysis);

        renameReportFile(inferOutUri, "left-to-right-report.txt");

        System.out.println("Infer Executing right -> left...");
        System.out.println(rightToLeftAnalysis);
        executeCommand(projectPath, rightToLeftAnalysis);

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
}
