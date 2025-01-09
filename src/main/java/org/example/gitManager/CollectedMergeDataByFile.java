package org.example.gitManager;

import java.util.Set;

public class CollectedMergeDataByFile {
    private String filePath;
    private Set<Integer> leftAddedLines;
    private Set<Integer> rightAddedLines;

    public CollectedMergeDataByFile(String filePath, Set<Integer> leftAddedLines, Set<Integer> rightAddedLines) {
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
}
