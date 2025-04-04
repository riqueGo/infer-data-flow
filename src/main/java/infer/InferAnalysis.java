package infer;

import java.nio.file.Path;

import static infer.InferConstants.*;
import static org.example.utils.PathToString.resolvePath;
import static org.example.utils.Terminal.executeCommand;
import static org.example.utils.FileUtils.renameFile;

public class InferAnalysis {
    private String projectPath;
    private String buildCommand;
    private String outputPath;

    public InferAnalysis(String projectPath, String buildCommand) {
        this.projectPath = projectPath;
        this.buildCommand = buildCommand;
        outputPath = projectPath + INFER_OUT;
    }

    public void executeDataFlowAnalysis() {
        System.out.println("\nStarting Build...");
        if (build()) {
            System.out.println("\nStarting Analysis...");
            analysis();
        }
    }

    private boolean build() {
        String captureCommand = String.format("infer capture -- %s", buildCommand);
        System.out.println(captureCommand);
        try {
            executeCommand(projectPath, captureCommand);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    private void analysis() {
        Path inferSourcePackagePath = Path.of(WORKING_DIRECTORY, INFER_PACKAGE_PATH);

        String analyzeCommand = "infer analyze --pulse-only";
        String rightToLeftAnalysis = String.format("%s --inferconfig-path %s", analyzeCommand, inferSourcePackagePath.resolve(INFER_CONFIG_RIGHT_TO_LEFT));
        String leftToRightAnalysis = String.format("%s --inferconfig-path %s", analyzeCommand, inferSourcePackagePath.resolve(INFER_CONFIG_LEFT_TO_RIGHT));

        System.out.println("Infer Executing left -> right");
        System.out.println(leftToRightAnalysis);
        try {
            executeCommand(projectPath, leftToRightAnalysis);
        } catch (RuntimeException e) {
            System.out.println("Failed to execute left -> right");
        }

        renameFile(outputPath,"report.txt", "left-to-right-report.txt");

        System.out.println("Infer Executing right -> left...");
        System.out.println(rightToLeftAnalysis);
        try {
            executeCommand(projectPath, rightToLeftAnalysis);
        } catch (RuntimeException e) {
            System.out.println("Failed to execute right -> left");
        }

        renameFile(outputPath,"report.txt", "right-to-left-report.txt");
    }

    public String getOutputPath() {
        return this.outputPath;
    }
}
