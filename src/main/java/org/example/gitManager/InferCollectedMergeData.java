package org.example.gitManager;

import project.Project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InferCollectedMergeData {
    private Project project;
    private String filePath;
    private List<String> classNames;
    private Set<Integer> leftAddedLines;
    private Set<Integer> rightAddedLines;

    public InferCollectedMergeData(Project project, String filePath, Set<Integer> leftAddedLines, Set<Integer> rightAddedLines) {
        this.project = project;
        this.filePath = filePath;
        this.leftAddedLines = leftAddedLines;
        this.rightAddedLines = rightAddedLines;
        this.classNames = new ArrayList<>();
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

    public String getFileName() {return  Path.of(filePath).getFileName().toString(); }

    public void addLeftAddedLines(Set<Integer> newLeftAddedLines) {
        leftAddedLines.addAll(newLeftAddedLines);
    }

    public void addRightAddedLines(Set<Integer> newRightAddedLines) {
        rightAddedLines.addAll(newRightAddedLines);
    }

    public void addClassName(String className) {
        classNames.add(className);
    }
}
