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
        System.out.println("Getting files changed by left and right...");
        CommitManager commitManager = new CommitManager(args.getHead(), args.getParents(), args.getBase());
        Project project = new Project("project", args.getTargetProjectRoot());
        ModifiedLinesManager modifiedLinesManager = new ModifiedLinesManager(args.getSsmDependenciesPath());
        MergeCommit mergeCommit = commitManager.buildMergeCommit();

        List<CollectedMergeDataByFile> collectedMergeDataByFiles = modifiedLinesManager.collectLineDataByFile(project, mergeCommit);

        System.out.println("Starting generate infer code...");
        InferGenerate inferGenerate = new InferGenerate(project.getPath());
        inferGenerate.createInferPackage(project.getPath());
        inferGenerate.generateInferCodeForEachCollectedMergeData(collectedMergeDataByFiles);

        System.out.println("Starting Analysis...");
        InferAnalysis inferAnalysis = new InferAnalysis();
        inferAnalysis.executeDataFlowAnalysis(project.getPath());
    }
}
