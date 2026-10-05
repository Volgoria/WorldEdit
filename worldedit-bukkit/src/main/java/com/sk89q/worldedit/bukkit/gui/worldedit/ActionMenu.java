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

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.bukkit.gui.Button;
import com.sk89q.worldedit.bukkit.gui.ItemBuilder;
import com.sk89q.worldedit.bukkit.gui.Menu;
import com.sk89q.worldedit.bukkit.gui.Text;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A six-row menu showing one button per constant of a {@link GuiAction} enum,
 * with an optional size control, hollow toggle, pattern and mask buttons in
 * the bottom row.
 *
 * <p>Actions fill rows 1 to 4 (columns 1 to 7) in declaration order, unless
 * an explicit layout is given. The bottom row is laid out as: back (0),
 * pattern (1), size controls (2-4), hollow (5), mask (6), an extra control
 * (7) and close (8).</p>
 *
 * @param <A> the action type
 */
final class ActionMenu<A extends Enum<A> & GuiAction> extends Menu {

    private static final int[] GRID = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43,
    };
    private static final int BOTTOM = 5;

    /**
     * The size used to expand {@code {size}}.
     *
     * @param label the name shown, e.g. {@code "Brush size"}
     * @param getter reads the size from the player's state
     * @param setter stores the size in the player's state
     * @param limit reads the matching limit from WorldEdit's configuration
     */
    record SizeControl(String label, ToIntFunction<PlayerGuiState> getter, ObjIntConsumer<PlayerGuiState> setter,
                       ToIntFunction<LocalConfiguration> limit) {
    }

    /**
     * Whether hollow shapes are requested.
     *
     * @param getter reads the flag
     * @param setter stores the flag
     * @param description what the toggle affects
     */
    record HollowControl(Predicate<PlayerGuiState> getter, BiConsumer<PlayerGuiState, Boolean> setter,
                         String description) {
    }

    /**
     * Adds menu-specific buttons.
     */
    @FunctionalInterface
    interface Decorator {

        /**
         * Add buttons.
         *
         * @param menu the menu; use {@link ActionMenu#place(int, Button)}
         * @param viewer the viewer
         * @param context the viewer's choices
         */
        void decorate(ActionMenu<?> menu, Player viewer, CommandContext context);
    }

    private final WorldEditGui gui;
    private final List<A> actions;
    private final Map<A, Integer> layout;
    @Nullable
    private final SizeControl size;
    @Nullable
    private final HollowControl hollow;
    private final boolean maskButton;
    private final List<Decorator> decorators;
    private final String name;

    private ActionMenu(Builder<A> builder) {
        super(Text.DARK_GRAY + "WorldEdit » " + builder.title, 6);
        this.gui = builder.gui;
        this.actions = List.copyOf(builder.actions);
        this.layout = builder.layout;
        this.size = builder.size;
        this.hollow = builder.hollow;
        this.maskButton = builder.maskButton;
        this.decorators = List.copyOf(builder.decorators);
        this.name = builder.title.toLowerCase(Locale.ROOT);
    }

    static <A extends Enum<A> & GuiAction> Builder<A> builder(WorldEditGui gui, String title, Class<A> type) {
        return new Builder<>(gui, title, type);
    }

    /**
     * Put a button in a slot, for {@link Decorator}s.
     *
     * @param slot the slot
     * @param button the button
     */
    void place(int slot, Button button) {
        set(slot, button);
    }

    private int maxSize() {
        return size == null ? GuiCommands.HARD_MAX_SIZE : Buttons.effectiveMax(size.limit().applyAsInt(gui.config()));
    }

    @Override
    protected void build(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        int currentSize = 0;
        if (size != null) {
            currentSize = GuiCommands.clampSize(size.getter().applyAsInt(state), maxSize());
            size.setter().accept(state, currentSize);
        }
        boolean isHollow = hollow != null && hollow.getter().test(state);
        CommandContext context = CommandContext.of(state, currentSize, isHollow);

        for (int i = 0; i < actions.size(); i++) {
            A action = actions.get(i);
            Integer slot = layout.isEmpty() ? (i < GRID.length ? GRID[i] : null) : layout.get(action);
            if (slot != null) {
                set(slot, actionButton(action, context));
            }
        }

        Runnable refresh = () -> refresh(viewer);
        set(slot(BOTTOM, 0), Buttons.backToMain(gui));
        if (actions.stream().anyMatch(a -> a.spec().usesPattern())) {
            set(slot(BOTTOM, 1), Buttons.patternPicker(gui, state, "the " + name, this::open));
        }
        if (size != null) {
            int max = maxSize();
            ObjIntConsumer<PlayerGuiState> setter = size.setter();
            set(slot(BOTTOM, 2), Buttons.step(false, currentSize, v -> GuiCommands.clampSize(v, max),
                v -> setter.accept(state, v), refresh));
            set(slot(BOTTOM, 3), Buttons.sizeDisplay(currentSize, max, size.label()));
            set(slot(BOTTOM, 4), Buttons.step(true, currentSize, v -> GuiCommands.clampSize(v, max),
                v -> setter.accept(state, v), refresh));
        }
        if (hollow != null) {
            HollowControl control = hollow;
            set(slot(BOTTOM, 5), Buttons.toggle("Hollow", isHollow, control.description(),
                () -> control.setter().accept(state, !isHollow), refresh));
        }
        if (maskButton) {
            set(slot(BOTTOM, 6), Buttons.mask(gui, state, this::open));
        }
        set(slot(BOTTOM, 8), Buttons.close());
        for (Decorator decorator : decorators) {
            decorator.decorate(this, viewer, context);
        }

        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }

    private Button actionButton(A action, CommandContext context) {
        ActionSpec spec = action.spec();
        String preview = GuiCommands.action(action, context, null);
        List<String> lore = new ArrayList<>();
        lore.add(Text.GRAY + spec.description());
        lore.add("");
        if (spec.usesPattern()) {
            lore.add(Text.GRAY + "Pattern: " + Text.WHITE + context.pattern());
        }
        if (spec.usesMask()) {
            lore.add(Text.GRAY + "Mask: " + Text.WHITE + (context.mask() == null ? "any non-air block" : context.mask()));
        }
        if (spec.usesSize() && size != null) {
            lore.add(Text.GRAY + size.label() + ": " + Text.WHITE + context.size());
        }
        if (spec.supportsHollow()) {
            lore.add(Text.GRAY + "Hollow: " + Text.WHITE + (context.hollow() ? "yes" : "no"));
        }
        lore.add(Text.DARK_GRAY + preview);
        lore.add("");
        String verb = switch (spec.behavior()) {
            case BIND -> "bind to your held item";
            case RUN, RUN_AND_CLOSE -> "run";
        };
        lore.add(Text.YELLOW + (spec.needsInput() ? "Click to type the value in chat and " + verb : "Click to " + verb));
        return new Button(ItemBuilder.of(Buttons.material(spec.icon()))
            .name(Text.GOLD + spec.displayName())
            .lore(lore)
            .build(), (player, _) -> click(player, action, context));
    }

    private void click(Player player, A action, CommandContext context) {
        ActionSpec spec = action.spec();
        if (spec.behavior() == ActionSpec.Behavior.BIND && !HeldItems.ensureBindable(player)) {
            return;
        }
        if (spec.question() != null && spec.input() != null) {
            gui.ask(player, spec.question(), spec.input(), argument -> execute(player, action, context, argument),
                this::open);
            return;
        }
        execute(player, action, context, null);
    }

    private void execute(Player player, A action, CommandContext context, @Nullable String argument) {
        ActionSpec spec = action.spec();
        String command = GuiCommands.action(action, context, argument);
        switch (spec.behavior()) {
            case RUN -> {
                gui.run(player, command);
                refresh(player);
            }
            case RUN_AND_CLOSE -> {
                player.closeInventory();
                gui.run(player, command);
            }
            case BIND -> {
                if (HeldItems.ensureBindable(player)) {
                    player.closeInventory();
                    gui.run(player, command);
                }
            }
            default -> throw new IllegalStateException("Unknown behavior " + spec.behavior());
        }
    }

    /**
     * Configures an {@link ActionMenu}.
     *
     * @param <A> the action type
     */
    static final class Builder<A extends Enum<A> & GuiAction> {

        private final WorldEditGui gui;
        private final String title;
        private final Class<A> type;
        private final List<A> actions;
        private Map<A, Integer> layout = Map.of();
        @Nullable
        private SizeControl size;
        @Nullable
        private HollowControl hollow;
        private boolean maskButton;
        private final List<Decorator> decorators = new ArrayList<>();

        private Builder(WorldEditGui gui, String title, Class<A> type) {
            this.gui = gui;
            this.title = title;
            this.type = type;
            this.actions = List.of(type.getEnumConstants());
            checkArgument(actions.size() <= GRID.length, "too many actions for the default layout");
        }

        /**
         * Place actions in explicit slots; actions without a slot are hidden.
         *
         * @param slots the slot of each action
         * @return this builder
         */
        Builder<A> layout(Map<A, Integer> slots) {
            Map<A, Integer> copy = new EnumMap<>(type);
            copy.putAll(slots);
            Map<Integer, A> seen = new HashMap<>();
            copy.forEach((action, slot) -> {
                checkArgument(slot >= 0 && slot < 45, "action slots must be above the bottom row");
                checkArgument(seen.put(slot, action) == null, "two actions share slot %s", slot);
            });
            this.layout = copy;
            return this;
        }

        Builder<A> size(String label, ToIntFunction<PlayerGuiState> getter, ObjIntConsumer<PlayerGuiState> setter,
                        ToIntFunction<LocalConfiguration> limit) {
            this.size = new SizeControl(label, getter, setter, limit);
            return this;
        }

        Builder<A> hollow(Predicate<PlayerGuiState> getter, BiConsumer<PlayerGuiState, Boolean> setter,
                          String description) {
            this.hollow = new HollowControl(getter, setter, description);
            return this;
        }

        Builder<A> maskButton() {
            this.maskButton = true;
            return this;
        }

        Builder<A> decorate(Decorator decorator) {
            decorators.add(decorator);
            return this;
        }

        ActionMenu<A> build() {
            return new ActionMenu<>(this);
        }
    }
}
