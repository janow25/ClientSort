/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.clientsort.client.order;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assigns related items (e.g. logs, planks and sticks) to a shared family, so that the
 * {@link SortOrder#ROWS} order can place them next to each other.
 */
public final class ItemFamilies {

    private static final Pattern WOOD = Pattern.compile(
            "^(?:stripped_)?(oak|spruce|birch|jungle|acacia|dark_oak|mangrove|cherry|pale_oak"
                    + "|bamboo|crimson|warped)_(?:log|wood|stem|hyphae|block|planks|stairs|slab"
                    + "|fence|fence_gate|door|trapdoor|sign|hanging_sign|boat|chest_boat|raft"
                    + "|chest_raft|button|pressure_plate)$");
    private static final Pattern METAL = Pattern.compile(
            "^(?:raw_|deepslate_|nether_)?(iron|gold|copper)(?:_ingot|_nugget|_block|_ore)?$");
    private static final Pattern GEM = Pattern.compile(
            "^(?:deepslate_)?(diamond|emerald|lapis)(?:_lazuli|_block|_ore)?$");

    private static final Pattern GEAR = Pattern.compile(
            "^(wooden|stone|iron|golden|diamond|netherite|copper|chainmail|leather|turtle)"
                    + "_(?:sword|pickaxe|axe|shovel|hoe|spear|helmet|chestplate|leggings|boots"
                    + "|horse_armor|nautilus_armor)$");
    private static final Pattern STONE = Pattern.compile(
            "^(?:cobbled_|polished_|smooth_|chiseled_|cracked_|mossy_|cut_)*"
                    + "(stone|cobblestone|granite|diorite|andesite|deepslate|blackstone|sandstone"
                    + "|red_sandstone|tuff)(?:_bricks?|_tiles)?(?:_stairs|_slab|_wall)?$");
    private static final Pattern GLASS = Pattern.compile(
            "^(?:[a-z_]+_stained_glass(?:_pane)?|glass(?:_pane)?|tinted_glass)$");
    private static final Pattern COLOR = Pattern.compile(
            "^(white|orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue"
                    + "|brown|green|red|black)_(?:dye|wool|carpet|bed)$");

    /**
     * Fixed families, mapped by item registry path.
     */
    private static final Map<String, String> FIXED = new HashMap<>();

    static {
        family("blaze", "blaze_rod", "blaze_powder");
        family("bone", "bone", "bone_meal", "bone_block");
        family("slime", "slime_ball", "slime_block");
        family("ender", "ender_pearl", "ender_eye");
        family("redstone", "redstone", "redstone_block", "redstone_ore", "deepslate_redstone_ore");
        family("glowstone", "glowstone_dust", "glowstone");
        family("clay", "clay_ball", "brick", "clay", "bricks");
        family("sugar", "sugar_cane", "sugar", "paper");
        family("coal", "coal", "charcoal", "coal_block", "coal_ore", "deepslate_coal_ore");
        family("ranged", "bow", "crossbow", "arrow", "spectral_arrow", "tipped_arrow", "shield");
        family("glass", "sand", "red_sand", "gravel");
        family("beef", "beef", "cooked_beef");
        family("porkchop", "porkchop", "cooked_porkchop");
        family("chicken", "chicken", "cooked_chicken");
        family("mutton", "mutton", "cooked_mutton");
        family("rabbit", "rabbit", "cooked_rabbit");
        family("cod", "cod", "cooked_cod");
        family("salmon", "salmon", "cooked_salmon");
        family("potato", "potato", "baked_potato", "poisonous_potato");
        family("wheat", "wheat", "bread");
        family("carrot", "carrot", "golden_carrot");
        family("apple", "apple", "golden_apple", "enchanted_golden_apple");
        family("beetroot", "beetroot", "beetroot_seeds", "beetroot_soup");
        family("redstone_tech", "repeater", "comparator", "piston", "sticky_piston", "observer",
                "hopper", "dispenser", "dropper", "rail", "powered_rail", "detector_rail",
                "activator_rail", "lever", "redstone_torch", "daylight_detector", "tripwire_hook");
        family("netherite", "netherite_scrap", "netherite_ingot", "netherite_block",
                "ancient_debris");
    }

    private ItemFamilies() {
    }

    private static void family(String name, String... paths) {
        for (String path : paths) {
            FIXED.put(path, name);
        }
    }

    /**
     * Resolves the family key of each item. Items without a family get a unique key, so that
     * they form a family of their own. Sticks join the first wood family that contains planks.
     *
     * @param items the items to resolve, in the order in which they should be considered.
     * @return a map of each item to its family key, in the order of {@code items}.
     */
    public static Map<Item, String> resolve(Collection<Item> items) {
        Map<Item, String> keys = new LinkedHashMap<>();
        for (Item item : items) {
            keys.put(item, keyOf(BuiltInRegistries.ITEM.getKey(item).getPath()));
        }

        if (keys.containsKey(Items.STICK)) {
            String target = null;
            for (Map.Entry<Item, String> entry : keys.entrySet()) {
                String path = BuiltInRegistries.ITEM.getKey(entry.getKey()).getPath();
                if (path.endsWith("_planks") && entry.getValue().startsWith("wood:")) {
                    target = entry.getValue();
                    break;
                }
            }
            if (target != null) {
                keys.put(Items.STICK, target);
            }
        }

        return keys;
    }

    private static String keyOf(String path) {
        String fixed = FIXED.get(path);
        if (fixed != null) return fixed;

        Matcher matcher = GEAR.matcher(path);
        if (matcher.matches()) return "gear:" + matcher.group(1);

        matcher = WOOD.matcher(path);
        if (matcher.matches()) return "wood:" + matcher.group(1);

        matcher = METAL.matcher(path);
        if (matcher.matches()) return "metal:" + matcher.group(1);

        matcher = GEM.matcher(path);
        if (matcher.matches()) return "gem:" + matcher.group(1);

        matcher = STONE.matcher(path);
        if (matcher.matches()) {
            String kind = matcher.group(1);
            return "stone:" + (kind.equals("cobblestone") ? "stone" : kind);
        }

        if (GLASS.matcher(path).matches()) return "glass";

        matcher = COLOR.matcher(path);
        if (matcher.matches()) return "color:" + matcher.group(1);

        return "item:" + path;
    }
}
