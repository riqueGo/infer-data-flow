package infer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Path;

import static infer.InferUtils.INFER_PACKAGE_PATH;

public class InferAnalysis {
    private InferConfig inferConfig;

    public InferAnalysis(InferConfig inferConfig) {
        this.inferConfig = inferConfig;
    }

    public void executeDataFlowAnalysis(String projectPath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(inferConfig);

        try (FileWriter writer = new FileWriter(Path.of(projectPath, INFER_PACKAGE_PATH, "inferConfig.json").toString())) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: Avoid Hardcode
        System.out.println("Infer Executing left -> right...");
        execInfer("infer --pulse-only --pulse-taint-config /home/rique/Documents/research/infer-data-flow-test/src/main/java/infer/inferConfig.json -o /home/rique/Documents/research/infer-data-flow-test/src/main/java/infer/infer-out-left-to-right -- /home/rique/Documents/research/infer-data-flow-test/gradlew clean build");
        inferConfig.swap();

        json = gson.toJson(inferConfig);

        try (FileWriter writer = new FileWriter(Path.of(projectPath, INFER_PACKAGE_PATH, "inferConfig.json").toString())) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: Avoid Hardcode
        System.out.println("Infer Executing right -> left...");
        execInfer("infer --pulse-only --pulse-taint-config /home/rique/Documents/research/infer-data-flow-test/src/main/java/infer/inferConfig.json -o /home/rique/Documents/research/infer-data-flow-test/src/main/java/infer/infer-out-right-to-left -- /home/rique/Documents/research/infer-data-flow-test/gradlew clean build");
    }

    private void execInfer(String command) {

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
