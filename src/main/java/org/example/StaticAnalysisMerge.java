package org.example;

import infer.InferAnalysis;
import org.example.config.Arguments;
import org.example.gitManager.CommitManager;
import org.example.gitManager.CollectedMergeDataByFile;
import org.example.gitManager.ModifiedLinesManager;
import infer.InferGenerate;
import project.MergeCommit;
import project.Project;

import java.nio.file.Path;
import java.util.*;

import static infer.InferConstants.WORKING_DIRECTORY;
import static org.example.utils.FileUtils.moveDirectory;

public class StaticAnalysisMerge {
    private final Arguments args;

    StaticAnalysisMerge(Arguments args) {
        this.args = args;
    }

    public void run() {
        System.out.println("Getting files changed...");
        Project project = new Project("project", args.getTargetProjectRoot());
        String projectPath = project.getPath();

        CommitManager commitManager = new CommitManager(args.getHead(), projectPath);
        System.out.println("\n Checkout to -> " + args.getHead());
        commitManager.checkoutToMergeCommit(projectPath);

        ModifiedLinesManager modifiedLinesManager = new ModifiedLinesManager(args.getSsmDependenciesPath());
        MergeCommit mergeCommit = commitManager.buildMergeCommit();

        List<CollectedMergeDataByFile> collectedMergeDataByFiles = modifiedLinesManager.collectLineDataByFile(project, mergeCommit);

        try{
            System.out.println("\nStarting generate infer code...");
            InferGenerate inferGenerate = new InferGenerate(collectedMergeDataByFiles);
            inferGenerate.generateInferCodeForEachCollectedMergeData(collectedMergeDataByFiles, args.getDepth());

            InferAnalysis inferAnalysis = new InferAnalysis(projectPath, args.getBuild());
            inferAnalysis.executeDataFlowAnalysis();

            System.out.println("\nMoving report files...");
            moveDirectory(Path.of(inferAnalysis.getOutputPath()), Path.of(WORKING_DIRECTORY, "reports", project.getOwnerAndName()[1], args.getHead()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nReseting Commit...");
        commitManager.resetAndCleanCommit(projectPath);
    }
}
