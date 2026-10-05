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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiCommandsTest {

    @Test
    void browsingNeedsTheListingPermission() {
        Set<String> granted = Set.of("worldedit.schematic.load", "worldedit.image.paste");
        // Loading schematics does not reveal the folder's contents, //schem list does
        assertFalse(GuiCommands.mayBrowse(granted::contains, GuiCommands.SCHEMATIC_BROWSE_PERMISSIONS));
        assertTrue(GuiCommands.mayBrowse(Set.of("worldedit.schematic.list")::contains,
            GuiCommands.SCHEMATIC_BROWSE_PERMISSIONS));
        assertTrue(GuiCommands.mayBrowse(granted::contains, GuiCommands.IMAGE_BROWSE_PERMISSIONS));
        assertFalse(GuiCommands.mayBrowse(Set.of("worldedit.gui")::contains, GuiCommands.IMAGE_BROWSE_PERMISSIONS));
    }

    @Test
    void brushCommands() {
        assertEquals("/brush sphere stone 5", GuiCommands.brush(BrushType.SPHERE, "stone", 5, false));
        assertEquals("/brush sphere -h stone 5", GuiCommands.brush(BrushType.SPHERE, "stone", 5, true));
        assertEquals("/brush cylinder -h stone,dirt 4 4",
            GuiCommands.brush(BrushType.CYLINDER, "stone,dirt", 4, true));
        assertEquals("/brush smooth 3 4", GuiCommands.brush(BrushType.SMOOTH, "stone", 3, false));
        assertEquals("/brush overlay grass_block 6", GuiCommands.brush(BrushType.OVERLAY, "grass_block", 6, false));
        assertEquals("/brush fill water 2", GuiCommands.brush(BrushType.FILL, "water", 2, false));
        assertEquals("/brush drain 7", GuiCommands.brush(BrushType.DRAIN, "stone", 7, false));
        assertEquals("/brush blob stone 5", GuiCommands.brush(BrushType.BLOB, "stone", 5, false));
        assertEquals("/brush raise sphere 5", GuiCommands.brush(BrushType.RAISE, "stone", 5, false));
        assertEquals("/brush clipboard -a", GuiCommands.brush(BrushType.CLIPBOARD, "stone", 5, false));
        assertEquals("/brush none", GuiCommands.unbindBrush());
    }

    @Test
    void lineThicknessIsSizeMinusOne() {
        assertEquals("/brush line stone 0", GuiCommands.brush(BrushType.LINE, "stone", 1, false));
        assertEquals("/brush line -h stone 2", GuiCommands.brush(BrushType.LINE, "stone", 3, true));
    }

    @Test
    void hollowIsIgnoredWhenUnsupported() {
        assertEquals("/brush blob stone 5", GuiCommands.brush(BrushType.BLOB, "stone", 5, true));
        assertEquals("//helix stone 4 8 3", GuiCommands.generate(GenerationShape.HELIX, "stone", 4, true));
    }

    @Test
    void brushMetadataMatchesTemplates() {
        assertTrue(BrushType.SPHERE.usesPattern());
        assertTrue(BrushType.SPHERE.supportsHollow());
        assertFalse(BrushType.SMOOTH.usesPattern());
        assertFalse(BrushType.CLIPBOARD.usesSize());
        assertTrue(BrushType.DRAIN.usesSize());
    }

    @ParameterizedTest
    @EnumSource(BrushType.class)
    void everyBrushExpandsAllPlaceholders(BrushType type) {
        String command = GuiCommands.brush(type, "stone", 5, true);
        assertTrue(command.startsWith("/brush " + type.getSubCommand()), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
        assertFalse(command.endsWith(" "), command);
    }

    @Test
    void generationCommands() {
        assertEquals("//sphere stone 5", GuiCommands.generate(GenerationShape.SPHERE, "stone", 5, false));
        assertEquals("//sphere -h glass 10", GuiCommands.generate(GenerationShape.SPHERE, "glass", 10, true));
        assertEquals("//cyl stone 5 5", GuiCommands.generate(GenerationShape.CYLINDER, "stone", 5, false));
        assertEquals("//cone -h stone 5 10", GuiCommands.generate(GenerationShape.CONE, "stone", 5, true));
        assertEquals("//pyramid sandstone 8", GuiCommands.generate(GenerationShape.PYRAMID, "sandstone", 8, false));
        assertEquals("//torus stone 9 3", GuiCommands.generate(GenerationShape.TORUS, "stone", 9, false));
        assertEquals("//torus stone 2 1", GuiCommands.generate(GenerationShape.TORUS, "stone", 2, false));
        assertEquals("//dome -h glass 6", GuiCommands.generate(GenerationShape.DOME, "glass", 6, true));
        assertEquals("//disk stone 4", GuiCommands.generate(GenerationShape.DISK, "stone", 4, false));
        assertEquals("//arch stone_bricks 10 5", GuiCommands.generate(GenerationShape.ARCH, "stone_bricks", 5, false));
    }

    @ParameterizedTest
    @EnumSource(GenerationShape.class)
    void everyShapeExpandsAllPlaceholders(GenerationShape shape) {
        String command = GuiCommands.generate(shape, "stone", 5, true);
        assertTrue(command.startsWith("//"), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
    }

    @Test
    void selectionCommands() {
        assertEquals("//wand", GuiCommands.selection(SelectionAction.WAND, "stone", null));
        assertEquals("//expand vert", GuiCommands.selection(SelectionAction.EXPAND_VERT, "stone", null));
        assertEquals("//set oak_planks", GuiCommands.selection(SelectionAction.SET, "oak_planks", null));
        assertEquals("//replace stone", GuiCommands.selection(SelectionAction.REPLACE, "stone", null));
        assertEquals("//replace stone", GuiCommands.selection(SelectionAction.REPLACE, "stone", " "));
        assertEquals("//replace dirt,grass_block stone",
            GuiCommands.selection(SelectionAction.REPLACE, "stone", "dirt,grass_block"));
        assertEquals("//walls cobblestone", GuiCommands.selection(SelectionAction.WALLS, "cobblestone", null));
        assertEquals("//wireframe glass", GuiCommands.selection(SelectionAction.WIREFRAME, "glass", null));
        assertEquals("//undo", GuiCommands.selection(SelectionAction.UNDO, "stone", "dirt"));
        assertEquals("//history", GuiCommands.selection(SelectionAction.HISTORY, "stone", null));
    }

    @ParameterizedTest
    @EnumSource(SelectionAction.class)
    void everySelectionActionExpandsAllPlaceholders(SelectionAction action) {
        String command = GuiCommands.selection(action, "stone", "dirt");
        assertTrue(command.startsWith("//"), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
    }

    @Test
    void schematicCommands() {
        assertEquals("/schem load castle.schem", GuiCommands.schematicLoad("castle.schem"));
        assertEquals("/schem load builds/castle.schem", GuiCommands.schematicLoad("builds/castle.schem"));
        assertEquals("/schem load \"my castle.schem\"", GuiCommands.schematicLoad("my castle.schem"));
        assertEquals("/schem save house", GuiCommands.schematicSave("house", false));
        assertEquals("/schem save -f house", GuiCommands.schematicSave("house", true));
    }

    @Test
    void quoting() {
        assertEquals("plain", GuiCommands.quote("plain"));
        assertEquals("\"\"", GuiCommands.quote(""));
        assertEquals("\"a b\"", GuiCommands.quote("a b"));
        assertEquals("\"say \\\"hi\\\"\"", GuiCommands.quote("say \"hi\""));
    }

    @Test
    void schematicNameValidation() {
        assertTrue(GuiCommands.isValidSchematicName("house"));
        assertTrue(GuiCommands.isValidSchematicName("my-house_2"));
        assertTrue(GuiCommands.isValidSchematicName("builds/house"));
        assertFalse(GuiCommands.isValidSchematicName(""));
        assertFalse(GuiCommands.isValidSchematicName("../secret"));
        assertFalse(GuiCommands.isValidSchematicName("/abs"));
        assertFalse(GuiCommands.isValidSchematicName("dir/"));
        assertFalse(GuiCommands.isValidSchematicName("a b"));
        assertFalse(GuiCommands.isValidSchematicName("house.schem"));
        assertFalse(GuiCommands.isValidSchematicName("x".repeat(65)));
    }

    @Test
    void singleArgument() {
        assertTrue(GuiCommands.isSingleArgument("50%stone,50%dirt"));
        assertTrue(GuiCommands.isSingleArgument("##wool"));
        assertFalse(GuiCommands.isSingleArgument(""));
        assertFalse(GuiCommands.isSingleArgument("stone dirt"));
    }

    @Test
    void clampSize() {
        assertEquals(1, GuiCommands.clampSize(0, 6));
        assertEquals(1, GuiCommands.clampSize(-4, 6));
        assertEquals(5, GuiCommands.clampSize(5, 6));
        assertEquals(6, GuiCommands.clampSize(9, 6));
        // Unlimited configuration falls back to the GUI's hard limit
        assertEquals(50, GuiCommands.clampSize(50, -1));
        assertEquals(GuiCommands.HARD_MAX_SIZE, GuiCommands.clampSize(1000, -1));
        assertEquals(GuiCommands.HARD_MAX_SIZE, GuiCommands.clampSize(1000, 500));
    }

    @Test
    void newBrushCommands() {
        assertEquals("/brush spline stone 2", GuiCommands.brush(BrushType.SPLINE, "stone", 3, false));
        assertEquals("/brush spline -h stone 2", GuiCommands.brush(BrushType.SPLINE, "stone", 3, true));
        assertEquals("/brush copypaste -a 6", GuiCommands.brush(BrushType.COPYPASTE, "stone", 6, true));
        assertEquals("/brush shatter air 8 8", GuiCommands.brush(BrushType.SHATTER, "air", 8, false));
        assertEquals("/brush surfacesplatter gravel 10 6 3",
            GuiCommands.brush(BrushType.SURFACE_SPLATTER, "gravel", 10, false));
        assertEquals("/brush layer 6 grass_block dirt stone", GuiCommands.action(BrushType.LAYER,
            new CommandContext("grass_block,dirt,stone", List.of("grass_block", "dirt", "stone"), null, 6, false),
            null));
        // A typed pattern is used as a single layer
        assertEquals("/brush layer 4 ##wool", GuiCommands.brush(BrushType.LAYER, "##wool", 4, false));
    }

    @Test
    void promptedBrushCommands() {
        CommandContext context = CommandContext.of("stone", 3, false);
        String commands = InputKind.TEXT.toArgument("//pos1 {x},{y},{z}; //pos2 {x},{y},{z}");
        assertEquals("/brush command -s 3 \"//pos1 {x},{y},{z}; //pos2 {x},{y},{z}\"",
            GuiCommands.action(BrushType.COMMAND, context, commands));
        assertEquals("/brush populateschem -r -a trees,#clipboard 3 5", GuiCommands.action(
            BrushType.POPULATE_SCHEMATIC, context, InputKind.FILE_LIST.toArgument("trees,#clipboard")));
        assertTrue(BrushType.COMMAND.spec().needsInput());
        assertTrue(BrushType.POPULATE_SCHEMATIC.spec().needsInput());
        assertFalse(BrushType.SPHERE.spec().needsInput());
        // Previews show a placeholder
        assertEquals("/brush command -s 3 " + CommandTemplate.INPUT_PREVIEW,
            GuiCommands.action(BrushType.COMMAND, context, null));
    }

    @Test
    void valuesAreNotExpandedAgain() {
        CommandContext context = CommandContext.of("{size}", 7, false);
        assertEquals("/brush command -s 7 \"{pattern} {size}\"",
            GuiCommands.action(BrushType.COMMAND, context, "\"{pattern} {size}\""));
        assertEquals("/brush sphere {size} 7", GuiCommands.brush(BrushType.SPHERE, "{size}", 7, false));
    }

    @Test
    void toolCommands() {
        CommandContext context = new CommandContext("oak_planks", List.of("oak_planks"), null, 12, false);
        assertEquals("/tool selwand", GuiCommands.action(ToolAction.SELECTION_WAND, context, null));
        assertEquals("/tool navwand", GuiCommands.action(ToolAction.NAVIGATION_WAND, context, null));
        assertEquals("/tool farwand", GuiCommands.action(ToolAction.FAR_WAND, context, null));
        assertEquals("/tool info", GuiCommands.action(ToolAction.INFO, context, null));
        assertEquals("/tool inspect", GuiCommands.action(ToolAction.INSPECT, context, null));
        assertEquals("/tool measure", GuiCommands.action(ToolAction.MEASURE, context, null));
        assertEquals("/tool tree", GuiCommands.action(ToolAction.TREE, context, null));
        assertEquals("/tool repl oak_planks", GuiCommands.action(ToolAction.REPLACER, context, null));
        assertEquals("/tool lrbuild oak_planks air", GuiCommands.action(ToolAction.LONG_RANGE_BUILD, context, null));
        assertEquals("/tool cycler", GuiCommands.action(ToolAction.CYCLER, context, null));
        assertEquals("/tool floodfill oak_planks 12", GuiCommands.action(ToolAction.FLOOD_FILL, context, null));
        assertEquals("/tool deltree", GuiCommands.action(ToolAction.DELETE_TREE, context, null));
        assertEquals("/tool stacker 12", GuiCommands.action(ToolAction.STACKER, context, null));
        assertEquals("/tool copypaste", GuiCommands.action(ToolAction.COPY_PASTE, context, null));
        assertEquals("/tool none", GuiCommands.unbindTool());
    }

    @Test
    void buildCommands() {
        CommandContext context = new CommandContext("stone_bricks", List.of("stone_bricks"), null, 9, true);
        assertEquals("//text -s 3 stone_bricks \"Hello world\"",
            GuiCommands.action(BuildAction.TEXT, context, InputKind.TEXT.toArgument(" Hello world ")));
        assertEquals("//symmetry", GuiCommands.action(BuildAction.SYMMETRY, context, null));
        assertEquals("//symmetry -a", GuiCommands.action(BuildAction.SYMMETRY_NO_AIR, context, null));
        assertEquals("//flatten", GuiCommands.action(BuildAction.FLATTEN, context, null));
        assertEquals("//terrain stone_bricks 32 9", GuiCommands.action(BuildAction.TERRAIN, context, null));
        assertEquals("//path stone_bricks 9", GuiCommands.action(BuildAction.PATH, context, null));
        assertEquals("//cave 3", GuiCommands.action(BuildAction.CAVE, context, null));
        assertEquals("//staircase stone_bricks 9", GuiCommands.action(BuildAction.STAIRCASE, context, null));
        assertEquals("//spiralstairs stone_bricks 9 32", GuiCommands.action(BuildAction.SPIRAL_STAIRS, context, null));
        assertEquals("//randomize stone_bricks 20", GuiCommands.action(BuildAction.RANDOMIZE, context, null));
    }

    @ParameterizedTest
    @EnumSource(ToolAction.class)
    void everyToolBindsAndExpands(ToolAction action) {
        assertEquals(ActionSpec.Behavior.BIND, action.spec().behavior());
        assertFullyExpanded(GuiCommands.action(action, CommandContext.of("stone", 5, true), null), "/tool ");
    }

    @ParameterizedTest
    @EnumSource(BuildAction.class)
    void everyBuildActionExpands(BuildAction action) {
        assertFullyExpanded(GuiCommands.action(action, CommandContext.of("stone", 5, true), "\"x\""), "//");
    }

    @ParameterizedTest
    @EnumSource(BrushType.class)
    void everyBrushBinds(BrushType type) {
        assertEquals(ActionSpec.Behavior.BIND, type.spec().behavior());
        assertFullyExpanded(GuiCommands.action(type, CommandContext.of("stone", 5, true), "x"), "/brush ");
    }

    private static void assertFullyExpanded(String command, String prefix) {
        assertTrue(command.startsWith(prefix), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
        assertFalse(command.endsWith(" ") || command.contains("  "), command);
    }

    @Test
    void schematicManagementCommands() {
        assertEquals("/schem info builds/castle.schem", GuiCommands.schematicInfo("builds/castle.schem"));
        assertEquals("/schem info \"my castle.schem\"", GuiCommands.schematicInfo("my castle.schem"));
        assertEquals("/schem rename castle.schem old/castle", GuiCommands.schematicRename("castle.schem", "old/castle"));
        assertEquals("/schem copy castle.schem castle-2", GuiCommands.schematicCopy("castle.schem", "castle-2"));
        assertEquals("/schem save house", GuiCommands.schematicSave("house", "sponge", false));
        assertEquals("/schem save house structure", GuiCommands.schematicSave("house", "structure", false));
        assertEquals("/schem save -f house obj", GuiCommands.schematicSave("house", "obj", true));
        assertEquals("/schem save house json", GuiCommands.schematicSave("house", "json", false));
    }

    @Test
    void saveFormatsCycle() {
        assertEquals("sponge", GuiCommands.SAVE_FORMATS.get(0));
        assertEquals("structure", GuiCommands.nextSaveFormat("sponge"));
        assertEquals("obj", GuiCommands.nextSaveFormat("structure"));
        assertEquals("json", GuiCommands.nextSaveFormat("obj"));
        assertEquals("sponge", GuiCommands.nextSaveFormat("json"));
        assertEquals("sponge", GuiCommands.nextSaveFormat("unknown"));

        PlayerGuiState state = new PlayerGuiState();
        assertEquals("sponge", state.getSaveFormat());
        state.setSaveFormat("obj");
        assertEquals("obj", state.getSaveFormat());
        state.setSaveFormat("../evil");
        assertEquals("sponge", state.getSaveFormat());
    }

    @Test
    void imageCommands() {
        assertEquals("//image logo.png", GuiCommands.image("logo.png", false, false));
        assertEquals("//image -v art/logo.png", GuiCommands.image("art/logo.png", true, false));
        assertEquals("//image -d logo.png", GuiCommands.image("logo.png", false, true));
        assertEquals("//image -v -d \"my logo.png\"", GuiCommands.image("my logo.png", true, true));
        assertEquals("//topview -s map", GuiCommands.topView("map"));
        assertEquals("//heightmap import island.png", GuiCommands.heightmapImport("island.png"));
        assertEquals("//heightmap export terrain/hills", GuiCommands.heightmapExport("terrain/hills"));
    }

    @Test
    void sessionCommands() {
        assertEquals("//gmask", GuiCommands.globalMask(null));
        assertEquals("//gmask", GuiCommands.globalMask(" "));
        assertEquals("//gmask #wall", GuiCommands.globalMask("#wall"));
        assertEquals("/mask", GuiCommands.brushMask(null));
        assertEquals("/mask #y[64][*]", GuiCommands.brushMask("#y[64][*]"));
        assertEquals("//limit", GuiCommands.limit(null));
        assertEquals("//limit -1", GuiCommands.limit(-1));
        assertEquals("//limit 50000", GuiCommands.limit(50000));
        assertEquals("//timeout", GuiCommands.timeout(null));
        assertEquals("//timeout 500", GuiCommands.timeout(500));
        assertEquals("//fast on", GuiCommands.fast(true));
        assertEquals("//fast off", GuiCommands.fast(false));
        assertEquals("//perf lighting off", GuiCommands.sideEffect("LIGHTING", false));
        assertEquals("//perf entity_ai on", GuiCommands.sideEffect("ENTITY_AI", true));
        assertEquals("//rotate 90", GuiCommands.rotate(90));
        assertEquals("//flip", GuiCommands.flip());
        assertEquals("/clearclipboard", GuiCommands.clearClipboard());
        assertEquals("//copy", GuiCommands.copy());
        assertEquals("//paste -a", GuiCommands.paste());
        assertEquals("//undo", GuiCommands.history(false, 1));
        assertEquals("//undo", GuiCommands.history(false, 0));
        assertEquals("//redo 5", GuiCommands.history(true, 5));
        assertEquals("//clearhistory", GuiCommands.clearHistory());
    }

    @Test
    void fileReferenceValidation() {
        assertTrue(GuiCommands.isValidFileReference("logo"));
        assertTrue(GuiCommands.isValidFileReference("art/logo.png"));
        assertFalse(GuiCommands.isValidFileReference("../logo.png"));
        assertFalse(GuiCommands.isValidFileReference("/etc/passwd"));
        assertFalse(GuiCommands.isValidFileReference("logo.tar.gz"));
        assertFalse(GuiCommands.isValidFileReference("my logo.png"));
        assertFalse(GuiCommands.isValidFileReference(""));

        assertTrue(GuiCommands.isValidFileList("trees"));
        assertTrue(GuiCommands.isValidFileList("trees,rocks/big.schem,#clipboard"));
        assertTrue(GuiCommands.isValidFileList("#CLIPBOARD"));
        assertFalse(GuiCommands.isValidFileList(""));
        assertFalse(GuiCommands.isValidFileList("trees,"));
        assertFalse(GuiCommands.isValidFileList(",trees"));
        assertFalse(GuiCommands.isValidFileList("trees,,rocks"));
        assertFalse(GuiCommands.isValidFileList("trees,../secret"));
        assertFalse(GuiCommands.isValidFileList("#other"));
    }

    @Test
    void textValidation() {
        assertTrue(GuiCommands.isSafeText("Hello world"));
        assertTrue(GuiCommands.isSafeText("//pos1 {x},{y},{z}; //set stone"));
        assertFalse(GuiCommands.isSafeText(" "));
        assertFalse(GuiCommands.isSafeText("say \"hi\""));
        assertFalse(GuiCommands.isSafeText("back\\slash"));
        assertFalse(GuiCommands.isSafeText("§ccolour"));
        assertFalse(GuiCommands.isSafeText("line\nbreak"));
        assertFalse(GuiCommands.isSafeText("x".repeat(GuiCommands.MAX_TEXT_LENGTH + 1)));
    }

    @Test
    void limitParsing() {
        assertEquals(-1, GuiCommands.parseLimit("-1"));
        assertEquals(0, GuiCommands.parseLimit("0"));
        assertEquals(100000, GuiCommands.parseLimit("100000"));
        assertNull(GuiCommands.parseLimit("-2"));
        assertNull(GuiCommands.parseLimit("ten"));
        assertNull(GuiCommands.parseLimit(""));
        assertNull(GuiCommands.parseLimit("99999999999"));
    }
}
