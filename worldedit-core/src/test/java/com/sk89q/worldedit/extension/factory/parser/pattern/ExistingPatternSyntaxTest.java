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

package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.factory.parser.ParserTestBase;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.ExtentBufferedCompositePattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.function.pattern.StateApplyingPattern;
import com.sk89q.worldedit.function.pattern.TypeApplyingPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the syntax and behaviour of the pattern parsers that predate this fork.
 */
class ExistingPatternSyntaxTest extends ParserTestBase {

    private final PatternFactory factory = WorldEdit.getInstance().getPatternFactory();

    private Pattern parse(String input) throws InputParseException {
        return factory.parseFromInput(input, newContext());
    }

    private List<String> suggest(String input) {
        return factory.getSuggestions(input, newContext());
    }

    private static ParserContext clipboardContext() {
        LocalSession session = new LocalSession();
        session.setClipboard(new ClipboardHolder(
            new BlockArrayClipboard(new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(1, 1, 1)))
        ));
        ParserContext context = newContext();
        context.setSession(session);
        return context;
    }

    @Test
    void singleBlock() throws InputParseException {
        assertInstanceOf(BaseBlock.class, parse("stone"));
        assertInstanceOf(BaseBlock.class, parse("minecraft:dirt"));
    }

    @Test
    void randomList() throws InputParseException {
        Pattern pattern = assertInstanceOf(RandomPattern.class, parse("stone,dirt"));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(idOf(pattern.applyBlock(BlockVector3.ZERO)));
        }
        assertEquals(Set.of("stone", "dirt"), seen);
    }

    @Test
    void randomWeights() throws InputParseException {
        Pattern pattern = assertInstanceOf(RandomPattern.class, parse("100%stone,0%dirt"));
        for (int i = 0; i < 50; i++) {
            assertEquals("stone", idOf(pattern.applyBlock(BlockVector3.ZERO)));
        }
        assertInstanceOf(RandomPattern.class, parse("2.5%stone,dirt"));
        assertInstanceOf(RandomPattern.class, parse("5.%stone,1%sand"));
        // the list is split before prefixes such as ^ are applied to each entry
        assertInstanceOf(RandomPattern.class, parse("^stone,dirt"));
        assertThrows(InputParseException.class, () -> parse("50%,dirt"));
        assertThrows(InputParseException.class, () -> parse("stone,nope"));
    }

    @Test
    void clipboard() throws InputParseException {
        assertThrows(InputParseException.class, () -> parse("#clipboard"));
        ParserContext context = clipboardContext();
        ClipboardPattern pattern = assertInstanceOf(ClipboardPattern.class, factory.parseFromInput("#clipboard", context));
        assertEquals(BlockVector3.ZERO, pattern.getOffset());
        ClipboardPattern offset = assertInstanceOf(ClipboardPattern.class, factory.parseFromInput("#copy@[1,-2,3]", context));
        assertEquals(BlockVector3.at(1, -2, 3), offset.getOffset());
        assertInstanceOf(ClipboardPattern.class, factory.parseFromInput("#COPY", context));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#copy@", context));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#copy@[1,2]", context));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#copy@[1,2,3", context));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#copy@[a,b,c]", context));
    }

    @Test
    void typeOrStateApplying() throws InputParseException {
        assertInstanceOf(TypeApplyingPattern.class, parse("^dirt"));
        assertInstanceOf(StateApplyingPattern.class, parse("^[facing=north]"));
        assertInstanceOf(ExtentBufferedCompositePattern.class, parse("^stone[facing=north]"));
        assertThrows(InputParseException.class, () -> parse("^["));
        assertThrows(InputParseException.class, () -> parse("^[facing]"));
        assertThrows(InputParseException.class, () -> parse("^[=north]"));
        assertThrows(InputParseException.class, () -> parse("^[facing=]"));
        assertThrows(InputParseException.class, () -> parse("^[a=b,,c=d]"));
        assertThrows(InputParseException.class, () -> parse("^[a=b,a=c]"));
    }

    @Test
    void suggestions() {
        assertTrue(suggest("").containsAll(List.of("^", "*", "#clipboard")));
        assertTrue(suggest("#cl").containsAll(List.of("#clipboard", "#clipboard@[x,y,z]")));
        assertTrue(suggest("#co").containsAll(List.of("#copy", "#copy@[x,y,z]")));
        assertTrue(suggest("#copy@").contains("#copy@[x,y,z]"));
        assertTrue(suggest("^#cl").contains("^#clipboard"));
        assertTrue(suggest("*").stream().allMatch(s -> s.startsWith("*")));
        assertTrue(suggest("stone,#cl").contains("stone,#clipboard"));
        assertTrue(suggest("50%#cl").contains("50%#clipboard"));
        assertTrue(suggest("stone,25%#cl").contains("stone,25%#clipboard"));
    }
}
