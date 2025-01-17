package org.example;

import infer.InferAnalysis;
import org.example.config.Arguments;
import org.example.gitManager.CommitManager;
import org.example.gitManager.CollectedMergeDataByFile;
import org.example.gitManager.ModifiedLinesManager;
import infer.InferGenerate;
import project.MergeCommit;
import project.Project;

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

        List<CollectedMergeDataByFile> collectedMergeDataByFiles = modifiedLinesManager.collectLineDataByFile(project, mergeCommit);
        InferGenerate inferGenerate = new InferGenerate(project.getPath());
        inferGenerate.generateInferCodeForEachCollectedMergeData(collectedMergeDataByFiles);

        InferAnalysis inferAnalysis = new InferAnalysis();
        inferAnalysis.executeDataFlowAnalysis(project.getPath());
    }
}
