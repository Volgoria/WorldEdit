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

package com.sk89q.worldedit.util.translation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that the translation keys used in the sources exist in {@code strings.json},
 * and that messages are given as many arguments as they have placeholders.
 */
class TranslationKeysTest {

    /**
     * Calls taking a translation key, with the index of the key argument.
     */
    private static final Map<String, Integer> KEY_CALLS = Map.of(
        "TranslatableComponent.of", 0,
        "announce", 1,
        "printAffected", 1
    );

    private static final Pattern CALL = Pattern.compile("(?<![\\w.])(TranslatableComponent\\.of|announce|printAffected)\\(");
    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    /**
     * Keys used by the sources but provided elsewhere (argument names are
     * rendered by Piston from its own names).
     */
    private static final Set<String> PROVIDED_ELSEWHERE = Set.of(
        "worldedit.argument.action"
    );

    private static Map<String, String> strings;

    @BeforeAll
    static void loadStrings() throws IOException {
        try (InputStream in = TranslationKeysTest.class.getResourceAsStream("/lang/strings.json")) {
            assertNotNull(in, "strings.json");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                strings = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {
                }.getType());
            }
        }
    }

    private static List<Path> sourceFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String project : List.of(".", "../worldedit-cli", "../worldedit-bukkit")) {
            Path root = Path.of(project, "src", "main", "java");
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }
        return files;
    }

    /**
     * Split the arguments of a call, starting just after its opening parenthesis.
     */
    private static List<String> arguments(String source, int start) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        int argStart = start;
        for (int i = start; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                for (i++; i < source.length() && source.charAt(i) != '"'; i++) {
                    if (source.charAt(i) == '\\') {
                        i++;
                    }
                }
            } else if (c == '\'') {
                i += source.charAt(i + 1) == '\\' ? 3 : 2;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) {
                    String last = source.substring(argStart, i).strip();
                    if (!last.isEmpty() || !args.isEmpty()) {
                        args.add(last);
                    }
                    return args;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                args.add(source.substring(argStart, i).strip());
                argStart = i + 1;
            }
        }
        throw new IllegalStateException("Unbalanced call");
    }

    private record KeyUse(String location, String key, List<String> messageArgs, boolean literalKey) {
    }

    private static List<KeyUse> keyUses() throws IOException {
        List<KeyUse> uses = new ArrayList<>();
        for (Path file : sourceFiles()) {
            String source = Files.readString(file);
            Matcher call = CALL.matcher(source);
            while (call.find()) {
                List<String> args = arguments(source, call.end());
                int keyIndex = KEY_CALLS.get(call.group(1));
                if (args.size() <= keyIndex) {
                    continue;
                }
                String keyArg = args.get(keyIndex);
                if (keyArg.contains("+") || !keyArg.contains("\"")) {
                    // Built at runtime, not checkable here
                    continue;
                }
                int line = source.substring(0, call.start()).split("\n", -1).length;
                String location = file.getFileName() + ":" + line;
                List<String> messageArgs = args.subList(keyIndex + 1, args.size()).stream()
                    .filter(arg -> !arg.startsWith("TextColor.") && !arg.startsWith("Style."))
                    .toList();
                boolean literalKey = LITERAL.matcher(keyArg).matches();
                Matcher literal = LITERAL.matcher(keyArg);
                while (literal.find()) {
                    uses.add(new KeyUse(location, literal.group(1), messageArgs, literalKey));
                }
            }
        }
        return uses;
    }

    @Test
    void everyUsedKeyExists() throws IOException {
        List<KeyUse> uses = keyUses();
        assertFalse(uses.isEmpty());
        List<String> missing = uses.stream()
            .filter(use -> !strings.containsKey(use.key()) && !PROVIDED_ELSEWHERE.contains(use.key()))
            // Argument names of commands are lower-case words without dots
            .filter(use -> use.key().contains("."))
            .map(use -> use.location() + " " + use.key())
            .toList();
        assertEquals(List.of(), missing);
    }

    @Test
    void messagesGetOneArgumentPerPlaceholder() throws IOException {
        List<String> problems = new ArrayList<>();
        for (KeyUse use : keyUses()) {
            String message = strings.get(use.key());
            if (message == null || !use.literalKey()
                || use.messageArgs().stream().anyMatch(arg -> !arg.contains("("))) {
                // Unknown key, or arguments passed through a variable such as an array
                continue;
            }
            int needed = 0;
            Matcher placeholder = PLACEHOLDER.matcher(message);
            while (placeholder.find()) {
                needed = Math.max(needed, Integer.parseInt(placeholder.group(1)) + 1);
            }
            if (needed != use.messageArgs().size()) {
                problems.add(use.location() + " " + use.key() + ": " + use.messageArgs().size()
                    + " arguments for " + needed + " placeholders");
            }
        }
        assertEquals(List.of(), problems);
    }
}
