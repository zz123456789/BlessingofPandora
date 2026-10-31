package com.blessingofpandora;

import com.blessingofpandora.client.ClientReversalPermission;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReversalDescriptions
{
    private static final ChatFormatting CONFIG_COLOR = ChatFormatting.YELLOW;
    private static final ChatFormatting LIVE_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting SOUL_ACQUISITION_COLOR =
            ChatFormatting.AQUA;

    private ReversalDescriptions()
    {
    }

    public static List<Component> steps(BlessingDefinitions definition)
    {
        ReversalSettings settings = ReversalSettings.display();
        List<Component> steps = new ArrayList<>(stepCount(definition));
        for (int i = 0; i < stepCount(definition); i++)
        {
            steps.add(step(definition, i, settings));
        }
        return List.copyOf(steps);
    }

    public static int stepCount(BlessingDefinitions definition)
    {
        return switch (definition)
        {
            case FREEDOM, FEARLESSNESS -> 3;
            case REQUIEM -> 4;
            default -> 2;
        };
    }

    /** JEI can request a single live step without reconstructing all of its siblings. */
    public static Component step(BlessingDefinitions definition, int index)
    {
        return step(definition, index, ReversalSettings.display());
    }

    private static Component step(BlessingDefinitions definition, int index, ReversalSettings settings)
    {
        if (index < 0 || index >= stepCount(definition))
        {
            throw new IndexOutOfBoundsException("Reversal step " + index + " for " + definition);
        }
        if (index == 0) return wearStep(definition);
        return switch (definition)
        {
            case FREEDOM, FEARLESSNESS -> index == 1
                    ? killChanceStep(definition, settings)
                    : killChanceRuleStep(definition, settings);
            case INFINITY -> infinityStep(settings);
            case SPIRIT -> spiritStep(settings);
            case STRENGTH -> strengthStep(settings);
            case REQUIEM -> switch (index)
            {
                case 1 -> requiemSoulStep();
                case 2 -> soulSculptingAcquisitionStep();
                default -> requiemExperienceStep(settings);
            };
            case LIBERATION -> liberationChanceStep(settings);
        };
    }

    public static Component condition(BlessingDefinitions definition)
    {
        return condition(definition, null);
    }

    public static Component condition(
            BlessingDefinitions definition, Component liveStep)
    {
        MutableComponent component = Component.empty();
        ReversalSettings settings = ReversalSettings.display();
        for (int i = 0; i < stepCount(definition); i++)
        {
            if (i > 0) component.append(" ");
            component.append(step(definition, i, settings));
        }
        if (liveStep != null
                && (definition == BlessingDefinitions.LIBERATION
                || definition == BlessingDefinitions.REQUIEM))
        {
            component.append(" ").append(liveStep);
        }
        return component;
    }

    public static Component liveLiberationStep(long curseCount, double chancePercent)
    {
        return liveLiberationStep(curseCount, chancePercent, false);
    }

    public static Component liveLiberationStep(long curseCount, double chancePercent, boolean heldPreview)
    {
        return Component.translatable(
                heldPreview ? "jei.blessingofpandora.reversal_liberation.held_preview"
                        : "jei.blessingofpandora.reversal_liberation.live",
                liveValue(formatNumber(curseCount)),
                liveValue(formatNumber(chancePercent)));
    }

    public static Component liveRequiemStep(long absorbedExperience)
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_requiem.live",
                liveValue(formatNumber(absorbedExperience)));
    }

    public static Component liveRequiemNotWearing()
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_requiem.live.not_wearing");
    }

    public static Component soulSculptingAcquisitionStep()
    {
        return Component.translatable(
                        "blessingofpandora.effect.soul_sculpting_acquire")
                .withStyle(
                        SOUL_ACQUISITION_COLOR,
                        ChatFormatting.ITALIC);
    }

    public static Component result(BlessingDefinitions definition)
    {
        return Component.translatable(key(definition, "result"));
    }

    public static Component title(BlessingDefinitions definition)
    {
        return Component.translatable(key(definition, "title"));
    }

    public static String formatNumber(double value)
    {
        if (value == Math.floor(value))
        {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static Component wearStep(BlessingDefinitions definition)
    {
        return Component.translatable(key(definition, "step_1"));
    }

    private static Component killChanceStep(BlessingDefinitions definition, ReversalSettings settings)
    {
        boolean first = definition == BlessingDefinitions.FREEDOM;
        String mob = first ? settings.firstMob() : settings.fifthMob();
        double chance = KillReversalProgress.chancePercent(
                first ? settings.firstBaseChancePercent() : settings.fifthBaseChancePercent(),
                first ? settings.firstFailureBonusPercent() : settings.fifthFailureBonusPercent(),
                ClientReversalPermission.killFailures(definition));
        return Component.translatable(
                key(definition, "step_2"),
                ReversalConfigUtil.isAny(mob) ? anyEntity()
                        : configName(ReversalConfigUtil.displayEntityName(mob)),
                liveValue(formatNumber(chance)));
    }

    private static Component killChanceRuleStep(BlessingDefinitions definition, ReversalSettings settings)
    {
        boolean first = definition == BlessingDefinitions.FREEDOM;
        return Component.translatable(
                "jei.blessingofpandora.reversal_kill.chance_rule",
                configValue(first ? settings.firstBaseChancePercent() : settings.fifthBaseChancePercent()),
                configValue(first ? settings.firstFailureBonusPercent() : settings.fifthFailureBonusPercent()));
    }

    private static Component infinityStep(ReversalSettings settings)
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_infinity.step_2",
                configValue(settings.secondStartY()),
                configValue(settings.secondTargetY()));
    }

    private static Component spiritStep(ReversalSettings settings)
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_spirit.step_2",
                configName(ReversalConfigUtil.displayEffectName(
                        settings.thirdEffect())));
    }

    private static Component strengthStep(ReversalSettings settings)
    {
        Component maxHealth = configValue(
                settings.fourthMaxHealth());
        Component effect = configName(ReversalConfigUtil.displayEffectName(
                settings.fourthEffect()));
        if (ReversalConfigUtil.isAny(settings.fourthMob()))
        {
            return Component.translatable(
                    "jei.blessingofpandora.reversal_strength.step_2.any",
                    maxHealth, effect);
        }
        return Component.translatable(
                "jei.blessingofpandora.reversal_strength.step_2",
                configName(ReversalConfigUtil.displayEntityName(
                        settings.fourthMob())),
                maxHealth, effect);
    }

    private static Component requiemSoulStep()
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_requiem.step_2");
    }

    private static Component requiemExperienceStep(ReversalSettings settings)
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_requiem.step_3",
                configValue(settings.sixthExperience()));
    }

    private static Component liberationChanceStep(ReversalSettings settings)
    {
        return Component.translatable(
                "jei.blessingofpandora.reversal_liberation.step_2",
                configValue(
                        settings.seventhBaseChancePercent()),
                configValue(
                        settings.seventhCurseBonusPercent()));
    }

    private static Component anyEntity()
    {
        return configName(Component.translatable(
                "blessingofpandora.value.any_entity"));
    }

    private static Component configValue(double value)
    {
        return configValue(formatNumber(value));
    }

    private static Component configValue(int value)
    {
        return configValue(String.valueOf(value));
    }

    private static Component configValue(String value)
    {
        return Component.literal(value).withStyle(
                CONFIG_COLOR, ChatFormatting.BOLD);
    }

    private static Component liveValue(String value)
    {
        return Component.literal(value).withStyle(
                LIVE_COLOR, ChatFormatting.BOLD);
    }

    private static Component configName(Component component)
    {
        return component.copy().withStyle(
                CONFIG_COLOR, ChatFormatting.BOLD);
    }

    private static String key(BlessingDefinitions definition, String suffix)
    {
        String shortName = definition.id().substring("blessing_of_".length());
        return "jei.blessingofpandora.reversal_" + shortName + "." + suffix;
    }
}
