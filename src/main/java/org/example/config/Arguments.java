package org.example.config;

public class Arguments {
    private String hc;
    private String dp;
    private String tpr;
    private String build;
    private int depth;

    public Arguments(String hc, String dp, String tpr, String build, int depth) {
        this.hc = hc;
        this.dp = dp;
        this.tpr = tpr;
        this.build = build;
        this.depth = depth;
    }

    // Getters
    public String getHead() { return hc; }
    public String getSsmDependenciesPath() { return dp; }
    public String getTargetProjectRoot() { return tpr; }
    public String getBuild() { return build; }
    public int getDepth() { return depth; }
}

