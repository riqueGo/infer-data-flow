package org.example.gitManager;

import project.Project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CollectedMergeDataByFile {
    private Project project;
    private String filePath;
    private Set<Integer> leftAddedLines;
    private Set<Integer> rightAddedLines;

    public CollectedMergeDataByFile(Project project, String filePath, Set<Integer> leftAddedLines, Set<Integer> rightAddedLines) {
        this.project = project;
        this.filePath = filePath;
        this.leftAddedLines = leftAddedLines;
        this.rightAddedLines = rightAddedLines;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getWhoChangedTheLine(int lineNumber) {
        if(leftAddedLines.contains(lineNumber)) {
            return  "left";
        } else if(rightAddedLines.contains(lineNumber)) {
            return  "right";
        }
        return "";
    }

    public String getProjectPath() {return project.getPath();}

    public String getFileName() {return  Path.of(filePath).getFileName().toString(); }
}
