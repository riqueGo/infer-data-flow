package org.example;

import org.example.config.Arguments;
import org.example.gitManager.CollectedMergeMethodData;
import org.example.gitManager.CommitManager;
import org.example.gitManager.InferCollectedMergeData;
import org.example.gitManager.ModifiedLinesManager;
import org.example.infer.InferParser;
import project.MergeCommit;
import project.Project;

import java.io.IOException;
import java.util.*;

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

        List<CollectedMergeMethodData> collectedMergeMethodDataList = modifiedLinesManager.collectData(project, mergeCommit);
        List<InferCollectedMergeData> inferCollectedMergeDatas = getInferCollectedMergeData(collectedMergeMethodDataList);

        for (InferCollectedMergeData collectedMergeData : inferCollectedMergeDatas) {
            InferParser inferParser = new InferParser(collectedMergeData);
            try {
                inferParser.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<InferCollectedMergeData> getInferCollectedMergeData(List<CollectedMergeMethodData> collectedMergeMethodDataList) {
        HashMap<String, InferCollectedMergeData> inferCollectedMergeDataByClass = new HashMap<>();

        for (CollectedMergeMethodData collectedMergeMethodData : collectedMergeMethodDataList) {
            String className = collectedMergeMethodData.getClassName();
            InferCollectedMergeData inferCollectedMergeData;
            if (!inferCollectedMergeDataByClass.containsKey(className)) {
                inferCollectedMergeData = new InferCollectedMergeData(
                        collectedMergeMethodData.getProject(),
                        className,
                        collectedMergeMethodData.getFilePath(),
                        new HashSet<>(collectedMergeMethodData.getLeftAddedLines()),
                        new HashSet<>(collectedMergeMethodData.getRightAddedLines())
                );
                inferCollectedMergeDataByClass.put(className, inferCollectedMergeData);
            } else {
                inferCollectedMergeData = inferCollectedMergeDataByClass.get(className);
                inferCollectedMergeData.addLeftAddedLines(collectedMergeMethodData.getLeftAddedLines());
                inferCollectedMergeData.addRightAddedLines(collectedMergeMethodData.getRightAddedLines());
            }
        }
        return new ArrayList<>(inferCollectedMergeDataByClass.values());
    }
}
