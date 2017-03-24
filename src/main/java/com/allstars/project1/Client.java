package com.allstars.project1;


import java.util.ArrayList;
import org.apache.commons.cli.*;

public class Client {
    public static void publish(Resource r) {

    }

    public static void remove(Resource r) {

    }

    public static void share(Resource r) {

    }

    public static ArrayList<Resource> query(Resource template) {
        return new ArrayList<>();
    }

    public static void fetch(Resource template) {

    }

    public static void exchange(Resource template) {

    }

    public static CommandLine getOptions(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("channel").required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("debug").build());
        options.addOption(Option.builder("description").hasArg().type(String.class).build());
        options.addOption(Option.builder("exchange").build());
        options.addOption(Option.builder("fetch").build());
        options.addOption(Option.builder("host").required().hasArg().type(String.class).build());
        options.addOption(Option.builder("name").required().hasArg().type(String.class).build());
        options.addOption(Option.builder("owner").required().hasArg().type(String.class).build());
        options.addOption(Option.builder("port").required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("publish").build());
        options.addOption(Option.builder("query").build());
        options.addOption(Option.builder("remove").build());
        options.addOption(Option.builder("secret").hasArg().type(String.class).build());
        options.addOption(Option.builder("servers").hasArg().type(String.class).build());
        options.addOption(Option.builder("share").build());
        options.addOption(Option.builder("tags").hasArg().type(String.class).build());
        options.addOption(Option.builder("uri").hasArg().type(String.class).build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("parse exception");
        } finally {
            return cmd;
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = getOptions(args);

        if (cmd.hasOption("publish")) {

        } else if (cmd.hasOption("remove")) {

        } else if (cmd.hasOption("share")) {

        } else if (cmd.hasOption("query")) {

        } else if (cmd.hasOption("fetch")) {

        }

    }
}