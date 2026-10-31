package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/** Default names follow blessing identity even when display.Name is lost or stale. */
public final class BlessingNames
{
    private BlessingNames()
    {
    }

    public static Component resolveDefaultName(ItemStack stack, Component name)
    {
        BlessingDefinitions definition = BlessingNbt.getBlessing(stack);
        if (definition == null || name.getStyle().isObfuscated()) return name;

        // Reuse vanilla's parsed component instead of parsing display.Name a second time.
        if (name.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals(definition.nameKey())) return name;

        String text = name.getString();
        String curseKey = stack.getItem().getDescriptionId(stack);
        boolean oldTranslation = name.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals(curseKey)
                && translated.getArgs().length == 0 && name.getSiblings().isEmpty();
        if (oldTranslation || text.isBlank() || text.equals(curseKey)
                || isLegacyCurseName(definition, text)
                || text.equals(stack.getItem().getName(stack).getString()))
        {
            // Display only: no NBT writes, per-tick repair, or extra inventory synchronization.
            return Component.translatable(definition.nameKey()).withStyle(ChatFormatting.GOLD);
        }
        return name;
    }

    private static boolean isLegacyCurseName(BlessingDefinitions definition, String text)
    {
        // Literal names can survive a language change. Match only the complete original
        // names from Curse of Pandora, leaving custom names and suffixes untouched.
        return switch (definition)
        {
            case FREEDOM -> text.equals("现实第一诅咒：囚笼") || text.equals("Curse Of Inertia");
            case INFINITY -> text.equals("现实第二诅咒：制限") || text.equals("Curse Of Proximity");
            case SPIRIT -> text.equals("现实第三诅咒：血肉") || text.equals("Curse Of Flesh");
            case STRENGTH -> text.equals("现实第四诅咒：羸弱") || text.equals("Curse Of Metabolism");
            case FEARLESSNESS -> text.equals("现实第五诅咒：恐惧") || text.equals("Curse Of Tension");
            case REQUIEM -> text.equals("现实第六诅咒：碎魂") || text.equals("Curse Of Prudence");
            case LIBERATION -> text.equals("现实第七诅咒：咒缚") || text.equals("Curse Of Spell");
        };
    }
}
