package org.example.gitManager;

import project.MergeCommit;
import project.Project;

import java.util.Set;

public class InferCollectedMergeData {
    private Project project;
    private String className;
    private String filePath;
    private Set<Integer> leftAddedLines;
    private Set<Integer> rightAddedLines;

    public InferCollectedMergeData(Project project, String className, String filePath, Set<Integer> leftAddedLines, Set<Integer> rightAddedLines) {
        this.project = project;
        this.className = className;
        this.filePath = filePath;
        this.leftAddedLines = leftAddedLines;
        this.rightAddedLines = rightAddedLines;
    }

    public String getFilePath() {
        return filePath;
    }

    public Set<Integer> getLeftAddedLines() {
        return leftAddedLines;
    }

    public Set<Integer> getRightAddedLines() {
        return rightAddedLines;
    }

    public String getProjectPath() {return project.getPath();}

    public void addLeftAddedLines(Set<Integer> newLeftAddedLines) {
        leftAddedLines.addAll(newLeftAddedLines);
    }

    public void addRightAddedLines(Set<Integer> newRightAddedLines) {
        rightAddedLines.addAll(newRightAddedLines);
    }
}
