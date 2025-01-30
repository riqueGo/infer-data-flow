package org.example.gitManager;

import groovy.lang.Tuple2;
import project.MergeCommit;
import project.Project;
import services.dataCollectors.modifiedLinesCollector.ModifiedLine;
import services.dataCollectors.modifiedLinesCollector.ModifiedLinesCollector;
import services.dataCollectors.modifiedLinesCollector.ModifiedMethod;
import services.dataCollectors.modifiedLinesCollector.ModifiedMethodsHelper;
import util.FileManager;
import util.TypeNameHelper;

import java.util.*;

public class ModifiedLinesManager {
    ModifiedLinesCollector modifiedLinesCollector;
    FileManager fileManager;
    ModifiedMethodsHelper modifiedMethodsHelper;

    public ModifiedLinesManager (String dependenciesPath) {
        modifiedLinesCollector = new ModifiedLinesCollector(dependenciesPath);
        modifiedMethodsHelper = new ModifiedMethodsHelper("diffj.jar", dependenciesPath);
    }

    public List<CollectedMergeMethodData> collectData(Project project, MergeCommit mergeCommit) {
        List<CollectedMergeMethodData> collectedMergeMethodDataList = new ArrayList<>();
        Set<String> mutuallyModifiedFiles = modifiedLinesCollector.getFilesModifiedByBothParents(project, mergeCommit);

        for (String filePath : mutuallyModifiedFiles) {
            Set<ModifiedMethod> allModifiedMethods = modifiedMethodsHelper.getModifiedMethods(project, filePath, mergeCommit.getAncestorSHA(), mergeCommit.getSHA());
            Map<String, Tuple2<ModifiedMethod, ModifiedMethod>> mutuallyModifiedMethods = modifiedLinesCollector.getMutuallyModifiedMethods(project, mergeCommit, filePath);

            if (mutuallyModifiedMethods.isEmpty()) {
                return collectedMergeMethodDataList;
            }

            for (ModifiedMethod method : allModifiedMethods) {
                Tuple2<ModifiedMethod, ModifiedMethod> leftAndRightMethods = mutuallyModifiedMethods.get(method.getSignature());

                boolean methodWasModifiedByBothParents = leftAndRightMethods != null;
                if (methodWasModifiedByBothParents) {
                    collectedMergeMethodDataList.add(this.collectMethodData(leftAndRightMethods, method, project, mergeCommit, filePath));
                }

            }

        }

        System.out.println(project.getName() + " - ModifiedLinesCollector collection finished");
        return collectedMergeMethodDataList;
    }

    public List<CollectedMergeDataByFile> collectLineDataByFile(Project project, MergeCommit mergeCommit) {
        Map<String, Set<Integer>> leftAddedLinesByFilePath = new HashMap<>();
        Map<String, Set<Integer>> rightAddedLinesByFilePath = new HashMap<>();
        Set<String> filePaths = new HashSet<>();

        for (String filePath : FileManager.getModifiedFiles(project, mergeCommit.getLeftSHA(), mergeCommit.getAncestorSHA(), "java")){
            Set<Integer> leftAddedLines = new HashSet<>();
            for (ModifiedLine modifiedLine : modifiedMethodsHelper.getModifiedLines(project, filePath, mergeCommit.getLeftSHA(), mergeCommit.getSHA())) {
                leftAddedLines.add(modifiedLine.getNumber());
            }
            if (!leftAddedLines.isEmpty()) {
                leftAddedLinesByFilePath.put(filePath, leftAddedLines);
                filePaths.add(filePath);
            }
        }
        for (String filePath : FileManager.getModifiedFiles(project, mergeCommit.getRightSHA(), mergeCommit.getAncestorSHA(), "java")){
            Set<Integer> rightAddedLines = new HashSet<>();
            for (ModifiedLine modifiedLine : modifiedMethodsHelper.getModifiedLines(project, filePath, mergeCommit.getRightSHA(), mergeCommit.getSHA())) {
                rightAddedLines.add(modifiedLine.getNumber());
            }
            if (!rightAddedLines.isEmpty()) {
                rightAddedLinesByFilePath.put(filePath, rightAddedLines);
                filePaths.add(filePath);
            }
        }

        List<CollectedMergeDataByFile> collectedMergeDataByFiles = new ArrayList<>();

        for (String filePath : filePaths) {
            Set<Integer> leftAddedLines = leftAddedLinesByFilePath.get(filePath);
            Set<Integer> rightAddedLines = rightAddedLinesByFilePath.get(filePath);
            collectedMergeDataByFiles.add(new CollectedMergeDataByFile(project.getPath() + filePath, leftAddedLines, rightAddedLines));
        }
        return collectedMergeDataByFiles;
    }

    private CollectedMergeMethodData collectMethodData(
            Tuple2<ModifiedMethod, ModifiedMethod> leftAndRightMethods,
            ModifiedMethod mergeMethod,
            Project project,
            MergeCommit mergeCommit,
            String filePath
    ) {
        String className = TypeNameHelper.getFullyQualifiedName(project, filePath, mergeCommit.getAncestorSHA());

        ModifiedMethod leftMethod = leftAndRightMethods.getV1();
        ModifiedMethod rightMethod = leftAndRightMethods.getV2();

        Set<Integer> leftAddedLines = new HashSet<Integer>();
        Set<Integer> leftDeletedLines = new HashSet<Integer>();
        Set<Integer> rightAddedLines = new HashSet<Integer>();
        Set<Integer> rightDeletedLines = new HashSet<Integer>();

        for (ModifiedLine mergeLine : mergeMethod.getModifiedLines()) {
            if (leftMethod.getModifiedLines().contains(mergeLine)) {
                if (mergeLine.getType() == ModifiedLine.ModificationType.Removed) {
                    leftDeletedLines.add(mergeLine.getNumber());
                } else {
                    leftAddedLines.add(mergeLine.getNumber());
                }
            }

            if (rightMethod.getModifiedLines().contains(mergeLine)) {
                if (mergeLine.getType() == ModifiedLine.ModificationType.Removed) {
                    rightDeletedLines.add(mergeLine.getNumber());
                } else {
                    rightAddedLines.add(mergeLine.getNumber());
                }
            }
        }

        CollectedMergeMethodData collectedMergeMethodData = new CollectedMergeMethodData(project, mergeCommit, className, project.getPath() + filePath, mergeMethod.getSignature(), leftAddedLines, leftDeletedLines, rightAddedLines, rightDeletedLines);
        System.out.println(collectedMergeMethodData.toString());

        return collectedMergeMethodData;
    }
}
