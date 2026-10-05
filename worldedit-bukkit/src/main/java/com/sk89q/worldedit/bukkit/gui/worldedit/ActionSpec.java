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

package com.sk89q.worldedit.bukkit.gui.worldedit;

import com.google.errorprone.annotations.Immutable;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes one button of an action menu: what it shows and which command
 * it runs.
 *
 * @param displayName the button name
 * @param description a one-line description
 * @param icon the Bukkit material name of the icon, e.g. {@code OAK_SAPLING}
 * @param template the command template, see {@link CommandTemplate}
 * @param behavior what the button does with the command
 * @param question the question asked in chat for {@code {input}}, or null
 * @param input how the answer is validated, or null without a question
 */
@Immutable
public record ActionSpec(String displayName, String description, String icon, String template,
                         Behavior behavior, @Nullable String question, @Nullable InputKind input) {

    /**
     * What a button does with its command.
     */
    public enum Behavior {
        /** Run the command and keep the menu open. */
        RUN,
        /** Close the menu, so the player can read the output, and run the command. */
        RUN_AND_CLOSE,
        /** Make sure the player holds an item that can take a tool, close the menu and run the command. */
        BIND
    }

    public ActionSpec {
        checkNotNull(displayName, "displayName");
        checkNotNull(description, "description");
        checkNotNull(icon, "icon");
        checkNotNull(template, "template");
        checkNotNull(behavior, "behavior");
        checkArgument(template.startsWith("/"), "template must start with a slash");
        checkArgument((question == null) == (input == null), "a question needs an input kind and vice versa");
        checkArgument(question == null || CommandTemplate.usesInput(template),
            "a question needs {input} in the template");
    }

    static ActionSpec run(String displayName, String description, String icon, String template) {
        return new ActionSpec(displayName, description, icon, template, Behavior.RUN, null, null);
    }

    static ActionSpec close(String displayName, String description, String icon, String template) {
        return new ActionSpec(displayName, description, icon, template, Behavior.RUN_AND_CLOSE, null, null);
    }

    static ActionSpec bind(String displayName, String description, String icon, String template) {
        return new ActionSpec(displayName, description, icon, template, Behavior.BIND, null, null);
    }

    /**
     * Get a copy that asks the player for {@code {input}} first.
     *
     * @param question the question
     * @param kind how to validate the answer
     * @return the new spec
     */
    ActionSpec ask(String question, InputKind kind) {
        return new ActionSpec(displayName, description, icon, template, behavior, question, kind);
    }

    public boolean needsInput() {
        return question != null;
    }

    /**
     * Check that the template uses {@code {input}} exactly when the player
     * is asked for it.
     *
     * @return true if consistent
     */
    public boolean isConsistent() {
        return CommandTemplate.usesInput(template) == needsInput();
    }

    public boolean usesPattern() {
        return CommandTemplate.usesPattern(template);
    }

    public boolean usesMask() {
        return CommandTemplate.usesMask(template);
    }

    public boolean usesSize() {
        return CommandTemplate.usesSize(template);
    }

    public boolean supportsHollow() {
        return CommandTemplate.supportsHollow(template);
    }
}
