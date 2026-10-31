package com.blessingofpandora;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import java.io.*;

/** Exercises production math, saved-data helpers and the wire codec without launching a world. */
public final class CoreRegressionChecks
{
    private static int checks;

    public static void main(String[] args) throws Exception
    {
        startingGiftChecks();
        benefitChecks();
        requiemAndLiberationChecks();
        BenefitOptimizationChecks.main(args);
        long accumulated = 0;
        for (int level = 0; level <= 10_000; level++)
        {
            check(ExperienceMath.atLevel(level) == accumulated, "level curve " + level);
            accumulated += level >= 30 ? 112 + (level - 30) * 9L
                    : level >= 15 ? 37 + (level - 15) * 5L : 7 + level * 2L;
        }
        check(ExperienceMath.available(0, 1.0f / 7, 7) == 1, "fractional XP rounding");
        check(ExperienceMath.available(100, 0, 742) == 30970, "levels without totalExperience");
        check(ExperienceMath.drain(50_000, 1_999_999, 2_000_000) == 1, "last point does not overcharge");
        check(ExperienceMath.drain(3, 0, 2_000_000) == 3, "available XP limit");
        check(ExperienceMath.drain(90_000, 0, 2_000_000) == 50_000, "per tick limit");
        check(ExperienceMath.drain(90_000, 100, 100) == 0, "already complete");
        check(ExperienceMath.drain(90_000, 500, 100) == 0, "lowered target");

        CompoundTag playerA = new CompoundTag();
        CompoundTag playerB = new CompoundTag();
        SixthCurseProgress.addStored(playerA, 123456, 2_000_000);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.write(playerA, new DataOutputStream(output));
        CompoundTag reloaded = NbtIo.read(new DataInputStream(new ByteArrayInputStream(output.toByteArray())));
        check(SixthCurseProgress.getStored(reloaded) == 123456, "progress survives NBT round trip");
        check(SixthCurseProgress.getStored(playerB) == 0, "other player/world isolated");
        check(SixthCurseProgress.addStored(reloaded, Long.MAX_VALUE, 2_000_000) == 2_000_000, "overflow safe cap");
        check(SixthCurseProgress.addStored(playerA, -10, 2_000_000) == 123456, "negative credit ignored");

        CompoundTag progress = new CompoundTag();
        check(PandoraProgress.restoredCount(progress, 4) == 4, "old save attribute migration");
        progress.putDouble("blessingofpandora:cursed_to_blessed", 5);
        check(PandoraProgress.restoredCount(progress.copy(), 0) == 5, "respawn saved count");
        progress.putBoolean("blessingofpandora:epilogue_granted", true);
        progress.putDouble("blessingofpandora:cursed_to_blessed", 0);
        check(PandoraProgress.restoredCount(progress, 0) == 7, "repair completed legacy count");

        ReversalSettings server = new ReversalSettings("minecraft:pig", 12.25, 3.75,
                500, -30, "minecraft:invisibility", 400, "minecraft:weakness", "any",
                "minecraft:pig", 42.5, 0.125, 4_000_000, 0.0123, 0.0456);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            server.encode(buffer);
            ReversalSettings decoded = ReversalSettings.decode(buffer);
            check(server.equals(decoded) && !buffer.isReadable(), "all server settings survive wire round trip");
            ReversalSettings.setServerSettings(decoded);
            check(ReversalSettings.display().sixthExperience() == 4_000_000, "custom XP setting retained");
            check(ReversalSettings.display().fifthMob().equals("minecraft:pig"), "custom mob retained");
            check(decoded.firstBaseChancePercent() == 12.25 && decoded.firstFailureBonusPercent() == 3.75
                    && decoded.fifthBaseChancePercent() == 42.5 && decoded.fifthFailureBonusPercent() == 0.125,
                    "independent fractional first and fifth probabilities survive wire round trip");
        }
        finally
        {
            ReversalSettings.setServerSettings(null);
            buffer.release();
        }
        killProbabilityChecks();
        System.out.println("PASS: " + checks + " core regression checks");
    }

    private static void killProbabilityChecks()
    {
        for (int failures = 0; failures < 1000; failures++)
        {
            check(KillReversalProgress.chancePercent(20, 10, failures) == Math.min(100, 20 + failures * 10),
                    "default kill probability and cap " + failures);
            check(KillReversalProgress.chancePercent(12.5, 0.125, failures) == Math.min(100, 12.5 + failures * 0.125),
                    "fractional percentage points " + failures);
        }
        check(KillReversalProgress.chancePercent(0, 0, Long.MAX_VALUE) == 0, "zero probabilities remain disabled");
        check(KillReversalProgress.chancePercent(20, 10, Long.MAX_VALUE) == 100, "large counters cap without overflow");
        check(KillReversalProgress.chancePercent(100, 0, 0) == 100, "certain base chance");
    }

    private static void requiemAndLiberationChecks()
    {
        RequiemCombo combo = new RequiemCombo();
        check(combo.layers(0) == 0, "first hit has no combo bonus");
        combo.hit(0);
        check(combo.layers(40) == 1, "two-second boundary retains combo");
        combo.hit(40);
        check(combo.layers(80) == 2, "confirmed hit renews expiry");
        check(combo.layers(81) == 0, "more than two seconds resets combo");
        for (int hit = 0; hit < 1000; hit++) combo.hit(100 + hit);
        check(combo.layers(1099) == 5, "combo is capped at five");
        check(new RequiemCombo().layers(1099) == 0, "other target or attacker starts independently");
        check(combo.layers(100) == 0, "world time rollback discards combo");
        check(BlessingBenefitMath.liberationReduction(0) == 0, "no enchantments no protection");
        check(BlessingBenefitMath.liberationReduction(-10) == 0, "invalid levels do not produce negative protection");
        check(BlessingBenefitMath.liberationReduction(100) == 0.2, "one hundred levels gives twenty percent");
        check(BlessingBenefitMath.liberationReduction(400) == 0.8
                && BlessingBenefitMath.liberationReduction(Integer.MAX_VALUE) == 0.8, "eight tenths cap without overflow");
        check(BlessingBenefitMath.finalDamage(1, 1000, true, 0) == 5, "half-percent maximum-health floor");
        check(BlessingBenefitMath.finalDamage(10, 1000, true, 0) == 10, "floor never reduces an existing hit");
        check(BlessingBenefitMath.finalDamage(1, 1000, true, 400) == 1, "protection applies after offensive floor");
        check(BlessingBenefitMath.finalDamage(0, 1000, true, 400) == 0, "floor does not revive canceled or absorbed damage");
        check(BlessingBenefitMath.finalDamage(10, Float.NaN, true, 0) == 10, "invalid target health does not spread NaN");
    }

    private static void benefitChecks()
    {
        var attribute = new net.minecraft.world.entity.ai.attributes.AttributeInstance(
                new net.minecraft.world.entity.ai.attributes.RangedAttribute("test", 4, 0, 1024), ignored -> {});
        var weaponId = java.util.UUID.fromString("fa233e1c-4180-4865-b01b-bcce9785aca3");
        var own = java.util.UUID.fromString("b1e55b1e-1600-4000-8000-000000000001");
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                weaponId, "slow weapon", -3.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                own, "freedom", 1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        for (int tick = 0; tick < 500; tick++)
        {
            check(BlessingBenefitMath.valueWithout(attribute, own, false) == 0.5, "threshold excludes own bonus and stays stable");
            check(attribute.getValue() == 1, "100% attack speed bonus");
        }
        attribute.removeModifier(own);
        check(attribute.getValue() == 0.5 && attribute.getModifier(weaponId) != null, "removal keeps other modifiers intact");

        var modifiers = java.util.List.of(
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(), "flat positive", 2,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(), "flat negative", -1,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(), "base positive", 0.4,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE),
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(), "base negative", -0.5,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE),
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(), "total positive", 0.5,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL),
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(), "complete immobilization", -1,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        check(BlessingBenefitMath.calculate(1, modifiers, null, false) == 0, "ordinary calculation preserves immobilization");
        check(Math.abs(BlessingBenefitMath.calculate(1, modifiers, null, true) - 6.3) < 1e-9,
                "full-food movement ignores penalties without Infinity and retains every positive modifier");
        for (int food = 0; food <= 20; food++)
        {
            check(BlessingBenefitMath.foodBonus(food, 4.99) == 0, "reality gate");
            double bonus = BlessingBenefitMath.foodBonus(food, 5);
            check(bonus >= 0 && bonus <= 0.4, "hunger can never produce a blessing penalty");
        }
        check(BlessingBenefitMath.foodBonus(16, 5) == 0, "food lower boundary");
        check(Math.abs(BlessingBenefitMath.foodBonus(19, 5) - 0.15) < 1e-9, "each food point contributes 5%");
        check(BlessingBenefitMath.foodBonus(20, 5) == 0.4, "full food adds the extra 20%");
        int fed = 0;
        for (int tick = 0; tick < 2399; tick++) fed = BlessingBenefitMath.fedTicks(fed, true);
        check(fed == 2399, "health buff not early");
        fed = BlessingBenefitMath.fedTicks(fed, true);
        check(fed == 2400 && BlessingBenefitMath.fedTicks(fed, true) == 2400, "health buff at two minutes with bounded counter");
        check(BlessingBenefitMath.fedTicks(fed, false) == 0, "removal or hunger resets timer");

        FearlessnessMarks marks = new FearlessnessMarks();
        marks.hit(0);
        check(marks.mature(200) == 0 && marks.mature(201) == 1, "mark maturity strict boundary");
        check(marks.mature(400) == 1 && marks.mature(401) == 0, "mark expiry strict boundary");
        marks.hit(500);
        check(marks.mature(499) == 0, "world clock rollback clears marks");
        var reference = new java.util.ArrayList<Long>();
        var random = new java.util.Random(160);
        for (long tick = 0; tick < 3000; tick++)
        {
            final long now = tick;
            reference.removeIf(created -> now - created > 400);
            int expected = (int) Math.min(5, reference.stream().filter(created -> now - created > 200).count());
            check(marks.mature(tick) == expected, "bounded marks match original unbounded list semantics");
            int hits = random.nextInt(20);
            for (int hit = 0; hit < hits; hit++) { marks.hit(tick); reference.add(tick); }
            check(marks.bucketCount() <= 401, "mark storage remains bounded under rapid hits");
        }
        check(new FearlessnessMarks().mature(3000) == 0, "targets and attackers have independent marks");
    }

    private static void startingGiftChecks() throws Exception
    {
        // Read the real dependency reward rather than duplicating its NBT layout in the test.
        CompoundTag necklace = new CompoundTag();
        try (var jar = new java.util.zip.ZipFile("curseofpandora-2.4.26.jar"))
        {
            var entry = jar.getEntry("data/curseofpandora/loot_tables/pandora_initial.json");
            try (var reader = new InputStreamReader(jar.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8))
            {
                var loot = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var item = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                        .getAsJsonArray("entries").get(0).getAsJsonObject();
                necklace.putString("id", item.get("name").getAsString());
                necklace.putByte("Count", (byte) 1);
                necklace.put("tag", net.minecraft.nbt.TagParser.parseTag(item.getAsJsonArray("functions")
                        .get(1).getAsJsonObject().get("tag").getAsString()));
            }
        }
        CompoundTag original = necklace.copy();
        var full = StartingGiftContents.configure(necklace, true, true);
        check(full.size() == 1 && full.get(0).equals(original), "default keeps original Pandora reward exactly");
        var empty = StartingGiftContents.configure(necklace, true, false);
        check(empty.size() == 1 && empty.get(0).getString("id").equals("pandora:pandora_necklace")
                && !empty.get(0).getCompound("tag").contains("PandoraCharms"), "necklace only contains no curses");
        var curses = StartingGiftContents.configure(necklace, false, true);
        check(curses.size() == 7, "no necklace gives seven loose curse items");
        for (int i = 0; i < 7; i++)
        {
            check(curses.get(i).getString("id").equals(BlessingDefinitions.values()[i].curseId().toString())
                    && curses.get(i).getByte("Count") == 1, "loose curse identity and count " + i);
        }
        check(StartingGiftContents.configure(necklace, false, false).isEmpty(), "both disabled gives nothing");
        check(necklace.equals(original), "configuration never mutates input reward");

        var config = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        BlessingOfPandoraConfig.SPEC.correct(config);
        check(Boolean.TRUE.equals(config.get("giveStartingNecklace"))
                && Boolean.TRUE.equals(config.get("giveStartingCurses")), "both gift defaults enabled");
        checkConfigComments(config);
        config.set("giveStartingNecklace", false);
        config.set("giveStartingCurses", false);
        config.set("reversal_sixth.experience", 4_000_000);
        BlessingOfPandoraConfig.SPEC.correct(config);
        check(Boolean.FALSE.equals(config.get("giveStartingNecklace"))
                && Boolean.FALSE.equals(config.get("giveStartingCurses"))
                && Integer.valueOf(4_000_000).equals(config.get("reversal_sixth.experience")), "valid custom settings survive correction");
        config.set("giveStartingNecklace", true);
        config.set("giveStartingCurses", true);
        config.set("reversal_sixth.experience", 2_000_000);
        var sample = java.nio.file.Path.of("build/reports/blessingofpandora.toml");
        java.nio.file.Files.createDirectories(sample.getParent());
        java.nio.file.Files.writeString(sample, new com.electronwill.nightconfig.toml.TomlWriter().writeToString(config));
    }

    private static void checkConfigComments(com.electronwill.nightconfig.core.CommentedConfig config)
    {
        for (var entry : config.entrySet())
        {
            check(config.getComment(entry.getKey()) != null && !config.getComment(entry.getKey()).isBlank(),
                    "config entry has explanatory comment: " + entry.getKey());
            if (entry.getValue() instanceof com.electronwill.nightconfig.core.CommentedConfig section)
                checkConfigComments(section);
        }
    }

    private static void check(boolean condition, String name)
    {
        checks++;
        if (!condition) throw new AssertionError(name);
    }
}
