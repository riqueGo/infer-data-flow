package org.example.gitManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MergeManager {

    public Process revertCommit(String baseCommit) throws IOException {
        System.out.println("==== REVERTING MERGE ====");
        Process proc  = Runtime.getRuntime().exec("git reset " + baseCommit + " --quiet");
        watchProcess(proc);

        Process proc1  = Runtime.getRuntime().exec("git checkout -- . ");
        watchProcess(proc1);
        return proc;
    }

    private void watchProcess(Process proc) throws IOException {
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        // Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
    }
}
