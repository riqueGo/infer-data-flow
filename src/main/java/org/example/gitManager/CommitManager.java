package org.example.gitManager;

import project.MergeCommit;

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

    public MergeCommit buildMergeCommit() {
        return new MergeCommit(this.head, this.parents, this.base);
    }
}
