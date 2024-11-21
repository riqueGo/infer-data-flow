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

        List<InferCollectedMergeData> inferCollectedMergeDatas = modifiedLinesManager.collectLineData(project, mergeCommit);

        for (InferCollectedMergeData collectedMergeData : inferCollectedMergeDatas) {
            InferParser inferParser = new InferParser(collectedMergeData);
            try {
                inferParser.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
