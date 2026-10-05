/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * The parsed command line arguments of the CLI.
 *
 * @param help whether usage information was requested
 * @param file the file to load, or {@code null} to ask the user with a dialog
 * @param script a script of commands to run before reading from standard input, or {@code null}
 * @param nonInteractive whether to exit after the script instead of reading from standard input
 */
record CLIArguments(boolean help, @Nullable Path file, @Nullable Path script, boolean nonInteractive) {

    static final String COMMAND_NAME = "java -jar worldedit-cli.jar";

    private static final Options OPTIONS = createOptions();

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(Option.builder("f")
            .longOpt("file")
            .hasArg()
            .argName("path")
            .desc("The file to load in. Either a schematic, or a level.dat in a world folder."
                + " If omitted, a file chooser dialog is shown.")
            .build());
        options.addOption(Option.builder("s")
            .longOpt("script")
            .hasArg()
            .argName("path")
            .desc("A file containing a list of commands to run, one per line."
                + " Blank lines and lines starting with '#' are ignored.")
            .build());
        options.addOption(Option.builder("n")
            .longOpt("non-interactive")
            .desc("Exit after running the script instead of reading further commands from standard input."
                + " Requires --script.")
            .build());
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("Show this help message and exit.")
            .build());
        return options;
    }

    /**
     * Parse the given command line arguments.
     *
     * @param args the arguments
     * @return the parsed arguments
     * @throws ParseException if the arguments are invalid
     */
    static CLIArguments parse(String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(OPTIONS, args);
        if (cmd.hasOption('h')) {
            return new CLIArguments(true, null, null, false);
        }
        if (cmd.getArgs().length > 0) {
            throw new ParseException("Unexpected argument(s): " + String.join(" ", Arrays.asList(cmd.getArgs())));
        }
        Path file = toPath(cmd.getOptionValue('f'), "file");
        Path script = toPath(cmd.getOptionValue('s'), "script");
        boolean nonInteractive = cmd.hasOption('n');
        if (nonInteractive && script == null) {
            throw new ParseException("--non-interactive requires a --script to run");
        }
        return new CLIArguments(false, file, script, nonInteractive);
    }

    @Nullable
    private static Path toPath(@Nullable String value, String name) throws ParseException {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new ParseException("The " + name + " path must not be empty");
        }
        try {
            return Paths.get(value);
        } catch (InvalidPathException e) {
            throw new ParseException("Invalid " + name + " path '" + value + "': " + e.getReason());
        }
    }

    /**
     * Get the usage/help text.
     *
     * @return the usage text
     */
    static String usage() {
        StringWriter out = new StringWriter();
        try (PrintWriter writer = new PrintWriter(out)) {
            new HelpFormatter().printHelp(
                writer,
                HelpFormatter.DEFAULT_WIDTH,
                COMMAND_NAME + " [options]",
                System.lineSeparator() + "Run WorldEdit commands against a schematic file."
                    + System.lineSeparator() + System.lineSeparator(),
                OPTIONS,
                HelpFormatter.DEFAULT_LEFT_PAD,
                HelpFormatter.DEFAULT_DESC_PAD,
                System.lineSeparator() + "Commands are read one per line; type 'stop' to save and exit.",
                false
            );
        }
        return out.toString();
    }
}
