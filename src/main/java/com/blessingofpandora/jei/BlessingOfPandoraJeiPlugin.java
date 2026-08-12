package com.blessingofpandora.jei;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.BlessingNbt;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
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
    public void registerRecipes(IRecipeRegistration registration)
    {
        List<ItemStack> ingredients = new ArrayList<>();
        ingredients.add(BlessingNbt.createStack(BlessingDefinitions.FREEDOM));

        Item curse = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.parse("curseofpandora:curse_of_inertia"));
        if (curse != null)
        {
            ingredients.add(new ItemStack(curse));
        }

        registration.addItemStackInfo(ingredients,
                Component.translatable("jei.blessingofpandora.reversal_freedom.title"),
                Component.translatable("jei.blessingofpandora.reversal_freedom.step_1"),
                Component.translatable("jei.blessingofpandora.reversal_freedom.step_2"),
                Component.translatable("jei.blessingofpandora.reversal_freedom.result"));

        List<ItemStack> infinityIngredients = new ArrayList<>();
        infinityIngredients.add(BlessingNbt.createStack(BlessingDefinitions.INFINITY));

        Item curseOfProximity = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.parse("curseofpandora:curse_of_proximity"));
        if (curseOfProximity != null)
        {
            infinityIngredients.add(new ItemStack(curseOfProximity));
        }

        registration.addItemStackInfo(infinityIngredients,
                Component.translatable("jei.blessingofpandora.reversal_infinity.title"),
                Component.translatable("jei.blessingofpandora.reversal_infinity.step_1"),
                Component.translatable("jei.blessingofpandora.reversal_infinity.step_2"),
                Component.translatable("jei.blessingofpandora.reversal_infinity.result"));

        addReversal(registration, BlessingNbt.createStack(BlessingDefinitions.SPIRIT),
                "curseofpandora:curse_of_flesh", "reversal_spirit.title",
                "reversal_spirit.step_1", "reversal_spirit.step_2", "reversal_spirit.result");
        addReversal(registration, BlessingNbt.createStack(BlessingDefinitions.STRENGTH),
                "curseofpandora:curse_of_metabolism", "reversal_strength.title",
                "reversal_strength.step_1", "reversal_strength.step_2", "reversal_strength.result");
        addReversal(registration, BlessingNbt.createStack(BlessingDefinitions.FEARLESSNESS),
                "curseofpandora:curse_of_tension", "reversal_fearlessness.title",
                "reversal_fearlessness.step_1", "reversal_fearlessness.step_2",
                "reversal_fearlessness.step_3", "reversal_fearlessness.result");
        addReversal(registration, BlessingNbt.createStack(BlessingDefinitions.REQUIEM),
                "curseofpandora:curse_of_prudence", "reversal_requiem.title",
                "reversal_requiem.step_1", "reversal_requiem.step_2",
                "reversal_requiem.step_3", "reversal_requiem.result");
        addReversal(registration, BlessingNbt.createStack(BlessingDefinitions.LIBERATION),
                "curseofpandora:curse_of_spell", "reversal_liberation.title",
                "reversal_liberation.step_1", "reversal_liberation.result");
    }

    private static void addReversal(
            IRecipeRegistration registration, ItemStack blessing, String curseId, String titleKey, String... infoKeys)
    {
        List<ItemStack> ingredients = new ArrayList<>();
        ingredients.add(blessing);

        Item curse = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(curseId));
        if (curse != null)
        {
            ingredients.add(new ItemStack(curse));
        }

        Component[] components = new Component[infoKeys.length];
        components[0] = Component.translatable("jei.blessingofpandora." + titleKey);
        for (int i = 1; i < infoKeys.length; i++)
        {
            components[i] = Component.translatable("jei.blessingofpandora." + infoKeys[i]);
        }
        registration.addItemStackInfo(ingredients, components);
    }
}
