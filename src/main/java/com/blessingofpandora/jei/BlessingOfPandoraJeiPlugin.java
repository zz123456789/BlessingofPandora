package com.blessingofpandora.jei;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.LoveOrbRegistration;
import net.minecraft.network.chat.Component;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

@JeiPlugin
public class BlessingOfPandoraJeiPlugin implements IModPlugin
{
    private static final ResourceLocation UID =
            ResourceLocation.parse(BlessingOfPandora.MODID + ":jei_plugin");

    @Override
    public ResourceLocation getPluginUid()
    {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        registration.addRecipeCategories(new ReversalRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        // JEI's information recipes flatten Components during registration.
        // Our category resolves live text while it is displayed.
        registration.addRecipes(ReversalRecipeCategory.TYPE, List.of(BlessingDefinitions.values()));
        if (LoveOrbRegistration.isAvailable())
            registration.addItemStackInfo(LoveOrbRegistration.createStack(),
                    Component.translatable("item.blessingofpandora.love_orb.obtain"),
                    Component.translatable("item.blessingofpandora.love_orb.effect"),
                    Component.translatable("item.blessingofpandora.love_orb.safety"),
                    Component.translatable("item.blessingofpandora.love.conditions"));
        if (LoveOrbRegistration.isAvailable())
            registration.addItemStackInfo(LoveOrbRegistration.createEternalStack(),
                    Component.translatable("item.blessingofpandora.love.evolve_obtain"),
                    Component.translatable("item.blessingofpandora.eternal_love.scope"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.1"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.2"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.3"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.4"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.5"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.6"),
                    Component.translatable("item.blessingofpandora.eternal_love.effect.7"),
                    Component.translatable("item.blessingofpandora.love.conditions"));
    }
}
