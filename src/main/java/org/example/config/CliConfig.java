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

        Option parentsOption = Option.builder("pc").argName("parents")
                .required().hasArgs().valueSeparator(' ').desc("the parents commits")
                .build();

        Option baseOption = Option.builder("bc").argName("base")
                .required().hasArg().desc("the base commit")
                .build();

        Option ssmDependenciesPathOption = Option.builder("dp").argName("ssmDependenciesPath")
                .required().hasArg().desc("path to ssm dependencies folder")
                .build();

        Option targetProjectRootOption = Option.builder("tpr").argName("targetProjectRoot").hasArg()
                .required().desc("path to target project root folder")
                .build();

        Option classNameOption = Option.builder("cn").argName("className").hasArg()
                .required().desc("packagename to main class. Eg: org.example.Main")
                .build();

        Option mainMethodOption = Option.builder("m").argName("mainMethod").hasArg()
                .desc("name of the main method. Eg: main")
                .build();

        Option gradlePathOption = Option.builder("gp").argName("gradlePath")
                .required().hasArg().desc("path to gradle bin")
                .build();


        Option mavenPathOption = Option.builder("mp").argName("mavenPath").hasArg()
                .required().desc("path to maven bin")
                .build();

        options.addOption(headOption);
        options.addOption(parentsOption);
        options.addOption(baseOption);
        options.addOption(ssmDependenciesPathOption);
        options.addOption(targetProjectRootOption);
        options.addOption(classNameOption);
        options.addOption(mainMethodOption);
        options.addOption(gradlePathOption);
        options.addOption(mavenPathOption);

        return options;
    }

    public Arguments parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String h = cmd.getOptionValue("hc");
        String[] p = cmd.getOptionValues("pc");
        String b = cmd.getOptionValue("bc");
        String dp = cmd.getOptionValue("dp");
        String tpr = cmd.getOptionValue("tpr");
        String cn = cmd.getOptionValue("cn");
        String m = cmd.getOptionValue("m");
        String gp = cmd.getOptionValue("gp");
        String mp = cmd.getOptionValue("mp");

        return new Arguments(h, p, b, dp, tpr, cn, m, gp, mp);
    }

    public Options getOptions() {
        return options;
    }
}
