package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.config.Arguments;
import org.example.config.InferConfig;
import org.example.gitManager.CommitManager;
import org.example.gitManager.InferCollectedMergeData;
import org.example.gitManager.ModifiedLinesManager;
import org.example.infer.InferParser;
import project.MergeCommit;
import project.Project;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.example.infer.InferParser.inferDependenciesPath;

public class StaticAnalysisMerge {
    private final Arguments args;

    StaticAnalysisMerge(Arguments args) {
        this.args = args;
    }

    public void run() {
        CommitManager commitManager = new CommitManager(args.getHead(), args.getParents(), args.getBase());
        Project project = new Project("project", args.getTargetProjectRoot());
        ModifiedLinesManager modifiedLinesManager = new ModifiedLinesManager(args.getSsmDependenciesPath());
        MergeCommit mergeCommit = commitManager.buildMergeCommit();

        InferParser.createInferPackage(project.getPath());

        List<InferCollectedMergeData> inferCollectedMergeDatas = modifiedLinesManager.collectLineData(project, mergeCommit);

        InferConfig inferConfig = new InferConfig();

        for (InferCollectedMergeData collectedMergeData : inferCollectedMergeDatas) {
            InferParser inferParser = new InferParser(collectedMergeData);
            try {
                inferParser.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO:Fix the second output report is overriding the first one
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(inferConfig);

        try (FileWriter writer = new FileWriter(Path.of(project.getPath(), inferDependenciesPath, "inferConfig.json").toString())) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        inferConfig.execInfer();

        inferConfig.swap();
        json = gson.toJson(inferConfig);

        try (FileWriter writer = new FileWriter(Path.of(project.getPath(), inferDependenciesPath, "inferConfig.json").toString())) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        inferConfig.execInfer();
    }
}
