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

package com.sk89q.worldedit.extension.factory.parser;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.registry.Registry;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.translation.TranslationManager;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for tests of pattern and mask parsers, which need a platform,
 * a translation manager and a few registered block types.
 */
public abstract class ParserTestBase extends BaseWorldEditTest {

    protected static final String[] BLOCKS = {
        "minecraft:air", "minecraft:stone", "minecraft:dirt", "minecraft:sand", "minecraft:glass",
    };

    @BeforeAll
    static void registerBlocks() {
        // error messages are rendered when parse exceptions are created
        TranslationManager translationManager = mock(TranslationManager.class);
        when(translationManager.convertText(any(Component.class), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(MOCKED_PLATFORM.getTranslationManager()).thenReturn(translationManager);
        for (String id : BLOCKS) {
            if (BlockType.REGISTRY.get(id) == null) {
                BlockType.REGISTRY.register(id, new BlockType(id));
            }
        }
    }

    @AfterAll
    static void clearBlocks() throws Exception {
        Field map = Registry.class.getDeclaredField("map");
        map.setAccessible(true);
        ((Map<?, ?>) map.get(BlockType.REGISTRY)).clear();
    }

    /**
     * Create a parser context that does not need an actor.
     *
     * @return the context
     */
    protected static ParserContext newContext() {
        ParserContext context = new ParserContext();
        context.setRestricted(false);
        context.setExtent(new NullExtent());
        return context;
    }

    /**
     * Get the short id of a block, e.g. {@code stone}.
     *
     * @param block the block
     * @return the id without namespace
     */
    protected static String idOf(BaseBlock block) {
        String id = block.getBlockType().id();
        return id.substring(id.indexOf(':') + 1);
    }
}
