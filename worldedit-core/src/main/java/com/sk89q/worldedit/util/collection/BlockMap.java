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

package com.sk89q.worldedit.util.collection;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.math.BitMath.fixSign;
import static com.sk89q.worldedit.math.BitMath.mask;

/**
 * A space-efficient map implementation for block locations.
 *
 * <p>Values are stored in 16x16x16 sections, which become dense arrays once well
 * filled. Iteration visits sections one at a time, in the order they were first
 * written to, and the positions within a dense section in x, then z, then y order.
 * Accessing a position in the same section as the previous access avoids hashing
 * entirely.</p>
 *
 * <p>X and Z coordinates are limited to 26 bits, and wrap around outside that
 * range. Y coordinates are unlimited.</p>
 */
public class BlockMap<V> extends AbstractMap<BlockVector3, V> {

    public static <V> BlockMap<V> create() {
        return new BlockMap<>(false);
    }

    /**
     * Create a map for {@link BaseBlock}s. Blocks without NBT data are stored, and
     * returned, as their state's shared {@link BaseBlock} instance. The map does
     * not accept {@code null} values.
     *
     * @return a new map
     */
    public static BlockMap<BaseBlock> createForBaseBlock() {
        return new BlockMap<>(true);
    }

    public static <V> BlockMap<V> copyOf(Map<? extends BlockVector3, ? extends V> source) {
        BlockMap<V> map = new BlockMap<>(false);
        map.putAll(source);
        return map;
    }

    /*
     * Sections are grouped into layers by the top 8 bits of y. Within a layer,
     * a section is keyed by 22 bits each of (x >> 4) and (z >> 4), and the low
     * 20 bits of (y >> 4).
     */

    private static final int LAYER_COUNT = 256;
    private static final long BITS_22 = mask(22);
    private static final long BITS_20 = mask(20);

    private static long sectionKey(int sectionX, int sectionY, int sectionZ) {
        return (sectionX & BITS_22)
            | ((sectionZ & BITS_22) << 22)
            | ((sectionY & BITS_20) << (22 + 22));
    }

    private static int layerIndex(int sectionY) {
        return (sectionY >> 20) & (LAYER_COUNT - 1);
    }

    /**
     * Stands in for a {@code null} value, as sections use {@code null} for absent values.
     */
    private static final Object NULL_VALUE = new Object();

    private final boolean baseBlocks;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final Long2ObjectLinkedOpenHashMap<BlockMapSection>[] layers =
        new Long2ObjectLinkedOpenHashMap[LAYER_COUNT];
    private int size;
    /**
     * The number of sections in the map.
     */
    private int sectionCount;
    /**
     * The number of sections that have held more than {@link BlockMapSection#DENSE_THRESHOLD}
     * values, an estimate of how many sections are well filled.
     */
    private int filledSections;

    /**
     * The most recently accessed section. Checked against the section's own final
     * coordinates, so concurrent reads remain safe.
     */
    private BlockMapSection lastSection;

    private Set<Entry<BlockVector3, V>> entrySet;
    private Set<BlockVector3> keySet;
    private Collection<V> values;

    private BlockMap(boolean baseBlocks) {
        this.baseBlocks = baseBlocks;
    }

    // Value encoding

    private Object encode(V value) {
        if (baseBlocks) {
            BaseBlock block = (BaseBlock) checkNotNull(value);
            if (block.getNbtReference() == null) {
                return block.toImmutableState().toBaseBlock();
            }
            return block;
        }
        return value == null ? NULL_VALUE : value;
    }

    @SuppressWarnings("unchecked")
    private V decode(Object raw) {
        return raw == NULL_VALUE ? null : (V) raw;
    }

    // Section access

    private BlockMapSection findSection(int sectionX, int sectionY, int sectionZ) {
        BlockMapSection section = lastSection;
        if (section != null && section.baseX >> 4 == sectionX && section.baseY >> 4 == sectionY
            && section.baseZ >> 4 == sectionZ) {
            return section;
        }
        Long2ObjectLinkedOpenHashMap<BlockMapSection> layer = layers[layerIndex(sectionY)];
        if (layer == null) {
            return null;
        }
        section = layer.get(sectionKey(sectionX, sectionY, sectionZ));
        if (section != null) {
            lastSection = section;
        }
        return section;
    }

    private BlockMapSection findOrCreateSection(int sectionX, int sectionY, int sectionZ) {
        BlockMapSection section = findSection(sectionX, sectionY, sectionZ);
        if (section != null) {
            return section;
        }
        int layerIndex = layerIndex(sectionY);
        Long2ObjectLinkedOpenHashMap<BlockMapSection> layer = layers[layerIndex];
        if (layer == null) {
            layers[layerIndex] = layer = new Long2ObjectLinkedOpenHashMap<>();
        }
        // If most sections are well filled, so will this one probably be. Starting
        // it dense skips building up the sparse table. In the worst case, this
        // allocates as many under-filled dense sections as there are filled ones.
        boolean dense = filledSections > 0 && filledSections * 2 >= sectionCount;
        section = new BlockMapSection(
            fixSign((int) ((sectionX & BITS_22) << 4), 26),
            sectionY << 4,
            fixSign((int) ((sectionZ & BITS_22) << 4), 26),
            dense
        );
        sectionCount++;
        layer.put(sectionKey(sectionX, sectionY, sectionZ), section);
        lastSection = section;
        return section;
    }

    private void removeSection(BlockMapSection section) {
        int sectionY = section.baseY >> 4;
        layers[layerIndex(sectionY)].remove(sectionKey(section.baseX >> 4, sectionY, section.baseZ >> 4));
        sectionRemoved(section);
    }

    private void sectionRemoved(BlockMapSection section) {
        sectionCount--;
        if (section.wasFilled()) {
            filledSections--;
        }
        if (lastSection == section) {
            lastSection = null;
        }
    }

    // Raw operations, by coordinates

    private Object getRaw(int x, int y, int z) {
        BlockMapSection section = findSection(x >> 4, y >> 4, z >> 4);
        return section == null ? null : section.get(BlockMapSection.index(x, y, z));
    }

    private Object putRaw(int x, int y, int z, Object raw) {
        BlockMapSection section = findOrCreateSection(x >> 4, y >> 4, z >> 4);
        Object old = section.put(BlockMapSection.index(x, y, z), raw);
        if (old == null) {
            size++;
            if (section.size() == BlockMapSection.DENSE_THRESHOLD + 1 && section.markFilled()) {
                filledSections++;
            }
        }
        return old;
    }

    private Object removeRaw(int x, int y, int z) {
        BlockMapSection section = findSection(x >> 4, y >> 4, z >> 4);
        if (section == null) {
            return null;
        }
        Object old = section.remove(BlockMapSection.index(x, y, z));
        if (old != null) {
            size--;
            if (section.isEmpty()) {
                removeSection(section);
            }
        }
        return old;
    }

    // Map operations

    @Override
    public V put(BlockVector3 key, V value) {
        return decode(putRaw(key.x(), key.y(), key.z(), encode(value)));
    }

    @Override
    public V get(Object key) {
        BlockVector3 vec = (BlockVector3) key;
        return decode(getRaw(vec.x(), vec.y(), vec.z()));
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        BlockVector3 vec = (BlockVector3) key;
        Object raw = getRaw(vec.x(), vec.y(), vec.z());
        return raw == null ? defaultValue : decode(raw);
    }

    @Override
    public boolean containsKey(Object key) {
        BlockVector3 vec = (BlockVector3) key;
        return getRaw(vec.x(), vec.y(), vec.z()) != null;
    }

    @Override
    public V remove(Object key) {
        BlockVector3 vec = (BlockVector3) key;
        return decode(removeRaw(vec.x(), vec.y(), vec.z()));
    }

    @Override
    public V putIfAbsent(BlockVector3 key, V value) {
        int x = key.x();
        int y = key.y();
        int z = key.z();
        Object old = getRaw(x, y, z);
        if (old == null || old == NULL_VALUE) {
            putRaw(x, y, z, encode(value));
            return null;
        }
        return decode(old);
    }

    @Override
    public boolean remove(Object key, Object value) {
        BlockVector3 vec = (BlockVector3) key;
        Object raw = getRaw(vec.x(), vec.y(), vec.z());
        if (raw == null || !Objects.equals(decode(raw), value)) {
            return false;
        }
        removeRaw(vec.x(), vec.y(), vec.z());
        return true;
    }

    @Override
    public boolean replace(BlockVector3 key, V oldValue, V newValue) {
        Object raw = getRaw(key.x(), key.y(), key.z());
        if (raw == null || !Objects.equals(decode(raw), oldValue)) {
            return false;
        }
        putRaw(key.x(), key.y(), key.z(), encode(newValue));
        return true;
    }

    @Override
    public V replace(BlockVector3 key, V value) {
        Object raw = getRaw(key.x(), key.y(), key.z());
        if (raw == null) {
            return null;
        }
        return decode(putRaw(key.x(), key.y(), key.z(), encode(value)));
    }

    // The compute methods look the position up again after calling the function,
    // which may itself modify this map.

    @Override
    public V computeIfAbsent(BlockVector3 key, Function<? super BlockVector3, ? extends V> mappingFunction) {
        V old = decode(getRaw(key.x(), key.y(), key.z()));
        if (old != null) {
            return old;
        }
        V value = mappingFunction.apply(key);
        if (value != null) {
            putRaw(key.x(), key.y(), key.z(), encode(value));
        }
        return value;
    }

    @Override
    public V computeIfPresent(BlockVector3 key,
                              BiFunction<? super BlockVector3, ? super V, ? extends V> remappingFunction) {
        V old = decode(getRaw(key.x(), key.y(), key.z()));
        if (old == null) {
            return null;
        }
        return storeComputed(key, remappingFunction.apply(key, old));
    }

    @Override
    public V compute(BlockVector3 key, BiFunction<? super BlockVector3, ? super V, ? extends V> remappingFunction) {
        V old = decode(getRaw(key.x(), key.y(), key.z()));
        return storeComputed(key, remappingFunction.apply(key, old));
    }

    @Override
    public V merge(BlockVector3 key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        checkNotNull(value);
        V old = decode(getRaw(key.x(), key.y(), key.z()));
        return storeComputed(key, old == null ? value : remappingFunction.apply(old, value));
    }

    private V storeComputed(BlockVector3 key, V value) {
        if (value != null) {
            putRaw(key.x(), key.y(), key.z(), encode(value));
        } else {
            removeRaw(key.x(), key.y(), key.z());
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super BlockVector3, ? super V> action) {
        for (Long2ObjectLinkedOpenHashMap<BlockMapSection> layer : layers) {
            if (layer == null) {
                continue;
            }
            for (BlockMapSection section : layer.values()) {
                for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
                    int index = section.indexAt(slot);
                    action.accept(
                        BlockVector3.at(section.blockX(index), section.blockY(index), section.blockZ(index)),
                        decode(section.valueAt(slot))
                    );
                }
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super BlockVector3, ? super V, ? extends V> function) {
        for (Long2ObjectLinkedOpenHashMap<BlockMapSection> layer : layers) {
            if (layer == null) {
                continue;
            }
            for (BlockMapSection section : layer.values()) {
                for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
                    int index = section.indexAt(slot);
                    V value = function.apply(
                        BlockVector3.at(section.blockX(index), section.blockY(index), section.blockZ(index)),
                        decode(section.valueAt(slot))
                    );
                    section.setValueAt(slot, encode(value));
                }
            }
        }
    }

    @Override
    public boolean containsValue(Object value) {
        for (Long2ObjectLinkedOpenHashMap<BlockMapSection> layer : layers) {
            if (layer == null) {
                continue;
            }
            for (BlockMapSection section : layer.values()) {
                for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
                    if (Objects.equals(decode(section.valueAt(slot)), value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void putAll(Map<? extends BlockVector3, ? extends V> m) {
        if (m instanceof BlockMap<?> other) {
            // optimize by skipping key construction
            for (Long2ObjectLinkedOpenHashMap<BlockMapSection> layer : other.layers) {
                if (layer == null) {
                    continue;
                }
                for (BlockMapSection section : layer.values()) {
                    for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
                        int index = section.indexAt(slot);
                        putRaw(section.blockX(index), section.blockY(index), section.blockZ(index),
                            encode(decode(section.valueAt(slot))));
                    }
                }
            }
        } else {
            super.putAll(m);
        }
    }

    @Override
    public void clear() {
        Arrays.fill(layers, null);
        lastSection = null;
        size = 0;
        sectionCount = 0;
        filledSections = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    // Views

    /**
     * Iterates over the values in the map, section by section.
     */
    private abstract class SlotIterator<T> implements Iterator<T> {

        private int layerIndex = -1;
        private ObjectIterator<Long2ObjectMap.Entry<BlockMapSection>> sectionIterator;
        private BlockMapSection section;
        private int slot = -1;
        private int nextSlot = -1;
        private boolean nextFound;
        private BlockMapSection lastReturnedSection;
        private int lastReturnedSlot;

        /**
         * Find the next slot holding a value, possibly in a later section.
         */
        private void findNext() {
            nextFound = true;
            if (section != null) {
                nextSlot = section.nextSlot(slot + 1);
                if (nextSlot >= 0) {
                    return;
                }
            }
            while (true) {
                if (sectionIterator != null && sectionIterator.hasNext()) {
                    section = sectionIterator.next().getValue();
                    nextSlot = section.nextSlot(0);
                    if (nextSlot >= 0) {
                        return;
                    }
                    continue;
                }
                section = null;
                do {
                    layerIndex++;
                } while (layerIndex < LAYER_COUNT && layers[layerIndex] == null);
                if (layerIndex >= LAYER_COUNT) {
                    nextSlot = -1;
                    return;
                }
                sectionIterator = layers[layerIndex].long2ObjectEntrySet().fastIterator();
            }
        }

        @Override
        public boolean hasNext() {
            if (!nextFound) {
                findNext();
            }
            return nextSlot >= 0;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextFound = false;
            slot = nextSlot;
            lastReturnedSection = section;
            lastReturnedSlot = slot;
            return create(section, slot);
        }

        abstract T create(BlockMapSection section, int slot);

        @Override
        public void remove() {
            BlockMapSection removeFrom = lastReturnedSection;
            if (removeFrom == null) {
                throw new IllegalStateException();
            }
            lastReturnedSection = null;
            removeFrom.removeAt(lastReturnedSlot);
            size--;
            // If hasNext() moved on to a later section, the empty section is left in
            // place, which is harmless, rather than disturbing the section iterator.
            if (removeFrom.isEmpty() && removeFrom == section) {
                sectionIterator.remove();
                sectionRemoved(removeFrom);
            }
        }
    }

    private final class EntryIterator extends SlotIterator<Entry<BlockVector3, V>> {
        @Override
        Entry<BlockVector3, V> create(BlockMapSection section, int slot) {
            return new LazyEntry(section, section.indexAt(slot), decode(section.valueAt(slot)));
        }
    }

    private final class KeyIterator extends SlotIterator<BlockVector3> {
        @Override
        BlockVector3 create(BlockMapSection section, int slot) {
            int index = section.indexAt(slot);
            return BlockVector3.at(section.blockX(index), section.blockY(index), section.blockZ(index));
        }
    }

    private final class ValueIterator extends SlotIterator<V> {
        @Override
        V create(BlockMapSection section, int slot) {
            return decode(section.valueAt(slot));
        }
    }

    @Override
    public Set<Entry<BlockVector3, V>> entrySet() {
        Set<Entry<BlockVector3, V>> es = entrySet;
        if (es == null) {
            entrySet = es = new AbstractSet<>() {
                @Override
                public Iterator<Entry<BlockVector3, V>> iterator() {
                    return new EntryIterator();
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public void clear() {
                    BlockMap.this.clear();
                }
            };
        }
        return es;
    }

    @Override
    public Set<BlockVector3> keySet() {
        Set<BlockVector3> ks = keySet;
        if (ks == null) {
            keySet = ks = new AbstractSet<>() {
                @Override
                public Iterator<BlockVector3> iterator() {
                    return new KeyIterator();
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public boolean contains(Object o) {
                    return containsKey(o);
                }

                @Override
                public boolean remove(Object o) {
                    BlockVector3 vec = (BlockVector3) o;
                    return removeRaw(vec.x(), vec.y(), vec.z()) != null;
                }

                @Override
                public void clear() {
                    BlockMap.this.clear();
                }
            };
        }
        return ks;
    }

    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            values = vs = new AbstractCollection<>() {
                @Override
                public Iterator<V> iterator() {
                    return new ValueIterator();
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public boolean contains(Object o) {
                    return containsValue(o);
                }

                @Override
                public void clear() {
                    BlockMap.this.clear();
                }
            };
        }
        return vs;
    }

    private final class LazyEntry implements Entry<BlockVector3, V> {

        private final BlockMapSection section;
        private final int index;
        private BlockVector3 lazyKey;
        private V value;

        private LazyEntry(BlockMapSection section, int index, V value) {
            this.section = section;
            this.index = index;
            this.value = value;
        }

        @Override
        public BlockVector3 getKey() {
            BlockVector3 result = lazyKey;
            if (result == null) {
                lazyKey = result = BlockVector3.at(section.blockX(index), section.blockY(index), section.blockZ(index));
            }
            return result;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            Object raw = encode(value);
            this.value = value;
            return decode(putRaw(section.blockX(index), section.blockY(index), section.blockZ(index), raw));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            return Objects.equals(getKey(), e.getKey()) && Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof BlockMap<?> other) {
            // optimize by skipping key construction
            if (other.size != size) {
                return false;
            }
            for (Long2ObjectLinkedOpenHashMap<BlockMapSection> layer : layers) {
                if (layer == null) {
                    continue;
                }
                for (BlockMapSection section : layer.values()) {
                    for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
                        int index = section.indexAt(slot);
                        Object otherRaw = other.getRaw(section.blockX(index), section.blockY(index), section.blockZ(index));
                        if (otherRaw == null || !Objects.equals(decode(section.valueAt(slot)), decode(otherRaw))) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return super.equals(o);
    }

    // satisfy checkstyle
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
