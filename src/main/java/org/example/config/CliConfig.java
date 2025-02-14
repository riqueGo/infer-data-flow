package org.example.config;

import org.apache.commons.cli.*;

public class CliConfig {
    private Options options;

    public CliConfig() {
        options = createOptions();
    }

    private Options createOptions() {
        Options options = new Options();

        Option headOption = Option.builder("hc").argName("head")
                .required().hasArg().desc("the head commit")
                .build();

        Option ssmDependenciesPathOption = Option.builder("dp").argName("ssmDependenciesPath")
                .required().hasArg().desc("path to ssm dependencies folder")
                .build();

        Option targetProjectRootOption = Option.builder("tpr").argName("targetProjectRoot").hasArg()
                .required().desc("path to target project root folder")
                .build();

        Option buildCommandOption = Option.builder("build").argName("buildCommand").hasArg()
                .required().desc("build command")
                .build();

        Option depthOption = Option.builder("depth").argName("depth")
                .hasArg().desc("interprocedural depth")
                .build();

        options.addOption(headOption);
        options.addOption(ssmDependenciesPathOption);
        options.addOption(targetProjectRootOption);
        options.addOption(buildCommandOption);
        options.addOption(depthOption);

        return options;
    }

    public Arguments parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String h = cmd.getOptionValue("hc");
        String dp = cmd.getOptionValue("dp");
        String tpr = cmd.getOptionValue("tpr");
        String build = cmd.getOptionValue("build");
        String depthArg = cmd.getOptionValue("depth");
        int depth = depthArg == null ? 0 : Integer.parseInt(depthArg);

        return new Arguments(h, dp, tpr, build, depth);
    }

    public Options getOptions() {
        return options;
    }
}
