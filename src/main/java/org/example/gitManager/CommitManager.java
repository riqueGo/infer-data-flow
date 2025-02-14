package org.example.gitManager;

import org.example.utils.Terminal;
import project.MergeCommit;

import java.util.List;

public class CommitManager {

    private String base;
    private String[] parents;
    private String head;

    public CommitManager(String[] args) {
        this(args[0], new String[]{args[1], args[2]}, args[3]);
    }

    public CommitManager(String head, String[] parents, String base) {
        this.head = head;
        this.parents = parents;
        this.base = base;
    }

    public CommitManager(String head, String projectPath) {
        this.head = head;
        setParents(projectPath);
        setBase(projectPath);
    }

    public MergeCommit buildMergeCommit() {
        return new MergeCommit(this.head, this.parents, this.base);
    }

    public void resetAndCleanCommit(String projectPath) {
        try {
            Terminal.executeCommand(projectPath, List.of("bash", "-c", "git reset --hard && git clean -fd"));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void resetCommit(String projectPath) {
        try {
            Terminal.executeCommand(projectPath, List.of("bash", "-c", "git reset --hard"));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void checkoutToMergeCommit(String projectPath) {
        try {
            Terminal.executeCommand(projectPath, List.of("bash", "-c", "git checkout " + this.head));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void setParents(String pathExecution) {
        try {
            parents = Terminal.executeCommandAndGetOutput(pathExecution, List.of("bash", "-c", "git log --pretty=%P -n 1 " + head)).getFirst().split(" ");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void setBase(String pathExecution) {
        try {
            base = Terminal.executeCommandAndGetOutput(pathExecution, List.of("bash", "-c", "git merge-base " + parents[0] + " " + parents[1])).getFirst();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
