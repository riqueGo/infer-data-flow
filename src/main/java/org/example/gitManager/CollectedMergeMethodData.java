package org.example.gitManager;

import project.MergeCommit;
import project.Project;

import java.util.Set;

public class CollectedMergeMethodData {
    private Project project;
    private MergeCommit mergeCommit;
    private String className;
    private String filePath;
    private String methodSignature;
    private Set<Integer> leftAddedLines;
    private Set<Integer> leftDeletedLines;
    private Set<Integer> rightAddedLines;
    private Set<Integer> rightDeletedLines;

    public CollectedMergeMethodData(
            Project project,
            MergeCommit mergeCommit,
            String className,
            String filePath,
            String methodSignature,
            Set<Integer> leftAddedLines,
            Set<Integer> leftDeletedLines,
            Set<Integer> rightAddedLines,
            Set<Integer> rightDeletedLines
    ) {
        this.project = project;
        this.mergeCommit = mergeCommit;
        this.className = className;
        this.filePath = filePath;
        this.methodSignature = methodSignature;
        this.leftAddedLines = leftAddedLines;
        this.leftDeletedLines = leftDeletedLines;
        this.rightAddedLines = rightAddedLines;
        this.rightDeletedLines = rightDeletedLines;
    }
}
