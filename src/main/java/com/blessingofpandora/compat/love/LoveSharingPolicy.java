package com.blessingofpandora.compat.love;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

/** Explicitly audited items; arbitrary third-party Curios callbacks never run as a maid. */
public final class LoveSharingPolicy
{
    private LoveSharingPolicy() {}
    private static final Set<String> LOOTING_ITEMS = Set.of(
            "charm_of_looting_1", "charm_of_looting_2", "charm_of_looting_3", "charm_of_looting_4");

    public static final Set<String> PANDORA_ITEMS = Set.of(
            "orb_of_stability", "golden_heart", "charm_of_calmness",
            "bless_of_snow_walker", "bless_of_lava_walker", "charm_of_night_vision",
            "orb_of_fire_rejection", "orb_of_master", "orb_of_nature",
            "orb_of_explosion_rejection", "orb_of_projectile_rejection", "orb_of_magic_rejection",
            "mini_beacon", "charm_of_health", "charm_of_armor", "charm_of_speed",
            "charm_of_damage", "charm_of_critical", "charm_of_archery", "charm_of_protection",
            "charm_of_magic", "charm_of_explosion", "charm_of_heavy_weapon", "charm_of_accuracy",
            "charm_of_luck", "orb_of_soulguard", "orb_of_executor", "orb_of_prosecutor",
            "angelic_wing", "angelic_bless", "angelic_descent", "angelic_protection", "angelic_punishment",
            "hellfire_skull", "hellfire_reformation", "eye_of_cursed_souls", "cursed_soul_crystal", "crown_of_demon",
            "shadow_core", "shadow_convergence", "shadow_consolidation", "shadow_reformation", "void_overflow",
            "abyssal_treasure", "abyssal_watcher", "abyssal_shell", "abyssal_crown", "abyssal_will",
            "barbaric_instinct", "barbaric_wrath", "barbaric_roar", "barbaric_blade", "primal_force",
            "evil_spirit_ritual", "evil_spirit_evoke", "evil_spirit_awakening", "evil_spirit_curse", "evil_spirit_walk",
            "wind_thrust", "earth_crush", "flaming_explosion", "waving_spell", "curse_redirection",
            "seal_of_swords", "spell_singularity", "mini_beacon_speed", "mini_beacon_haste",
            "mini_beacon_strength", "mini_beacon_jump", "mini_beacon_resistance", "mini_beacon_regen",
            "curse_of_inertia", "curse_of_proximity", "curse_of_flesh", "curse_of_metabolism",
            "curse_of_tension", "curse_of_prudence", "curse_of_spell", "illusory_affection", "fading_dream");

    /** Mechanism-level exclusions, not a blanket exclusion of every Player-typed upstream callback. */
    public static final Map<String, String> BLOCKED_MECHANISMS = Map.ofEntries(
            Map.entry("curios_slot_expansion", "Player inventory slots are not registered on maids."),
            Map.entry("player_food", "Vanilla Player FoodData has no maid equivalent; hunger-dependent bonuses are disabled."),
            Map.entry("player_reversal", "Experience absorption, reversal rolls, advancement and control UI remain owner-only."),
            Map.entry("player_input", "Client auto-attack, screen shake and camera/input behavior are not run on maids."),
            Map.entry("illusory_affection", "Enigmatic Legacy cursed-ring player aggro state is not transferred."),
            Map.entry("fading_dream", "Enigmatic Legacy player sleep/time statistics are not transferred."),
            Map.entry("seal_of_swords", "L2Hostility PlayerDifficulty capability is not created on maids."),
            Map.entry("evil_spirit_ritual", "Player-attributed experience-drop callback is not replayed or globally reassigned."),
            Map.entry("evil_spirit_evoke", "Upstream spirit owner and constructor require Player; no fake player or ownership rewrite."),
            Map.entry("l2hostility_looting", "Player loot-capability callbacks are not replayed for maids."),
            Map.entry("unknown_item", "Unaudited third-party callbacks are blocked until an explicit adapter exists."));

    public static boolean isKnown(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        if (id.getNamespace().equals("curseofpandora")) return PANDORA_ITEMS.contains(id.getPath());
        if (id.getNamespace().equals("blessingofpandora"))
            return id.getPath().equals("love_orb") || id.getPath().equals("eternal_love_orb");
        return id.getNamespace().equals("l2hostility") && LOOTING_ITEMS.contains(id.getPath());
    }
}
