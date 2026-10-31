package com.blessingofpandora;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.blessingofpandora.network.BlessingNetwork;

@Mod(BlessingOfPandora.MODID)
public class BlessingOfPandora
{
    public static final String MODID = "blessingofpandora";

    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    public static final RegistryObject<com.mojang.serialization.Codec<StartingGiftLootModifier>> STARTING_GIFT =
            LOOT_MODIFIERS.register("starting_gift", () -> StartingGiftLootModifier.CODEC);

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MODID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<Attribute> CURSED_TO_BLESSED =
            ATTRIBUTES.register("cursed_to_blessed",
                    () -> new RangedAttribute(
                            "attribute.name.blessingofpandora.cursed_to_blessed",
                            0.0, 0.0, 7.0).setSyncable(true));
    public static final RegistryObject<Attribute> STRONG =
            ATTRIBUTES.register("strong",
                    () -> new RangedAttribute(
                            "attribute.name.blessingofpandora.strong",
                            0.0, 0.0, 1024.0).setSyncable(true));
    public static final RegistryObject<Attribute> SOUL_DAMAGE_TAKEN =
            ATTRIBUTES.register("soul_sculpting_damage_taken",
                    () -> new RangedAttribute(
                            "attribute.name.blessingofpandora.soul_sculpting_damage_taken",
                            1.0, 0.0, 1024.0).setSyncable(true));
    public static final RegistryObject<MobEffect> SOUL_SCULPTING =
            MOB_EFFECTS.register("soul_sculpting",
                    () -> new SoulSculptingEffect(MobEffectCategory.BENEFICIAL, 0x88E8FF));
    public static final RegistryObject<Enchantment> PANDORA_CURSE =
            ENCHANTMENTS.register("pandora_curse",
                    () -> new PandoraCurseEnchantment(
                            Enchantment.Rarity.VERY_RARE,
                            EquipmentSlot.values()));
    public static final RegistryObject<Item> REVERSAL_CONTROL =
            ITEMS.register("reversal_control",
                    () -> new ReversalControlItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("blessing_of_pandora", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blessing_of_pandora"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> BlessingNbt.createStack(BlessingDefinitions.FREEDOM))
                    .displayItems((parameters, output) -> {
                        for (BlessingDefinitions definition : BlessingDefinitions.values())
                        {
                            ItemStack stack = BlessingNbt.createStack(definition);
                            if (!stack.isEmpty())
                            {
                                output.accept(stack);
                            }
                        }
                        output.accept(new ItemStack(REVERSAL_CONTROL.get()));
                        if (LoveOrbRegistration.isAvailable()) output.accept(LoveOrbRegistration.createStack());
                        if (LoveOrbRegistration.isAvailable()) output.accept(LoveOrbRegistration.createEternalStack());
                        output.accept(EnchantedBookItem.createForEnchantment(
                                new EnchantmentInstance(PANDORA_CURSE.get(), 1)));
                    })
                    .build());

    public BlessingOfPandora(FMLJavaModLoadingContext context)
    {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                BlessingOfPandoraConfig.SPEC,
                "blessingofpandora.toml");

        IEventBus modEventBus = context.getModEventBus();
        LOOT_MODIFIERS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        ATTRIBUTES.register(modEventBus);
        ENCHANTMENTS.register(modEventBus);
        LoveOrbRegistration.register(ITEMS);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(BlessingOfPandora::commonSetup);
        modEventBus.addListener(BlessingOfPandora::modifyAttributes);
        modEventBus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) ->
                com.blessingofpandora.compat.love.LoveSharing.invalidateConfig());
    }

    private static void commonSetup(FMLCommonSetupEvent event)
    {
        BlessingNetwork.register();
        BlessingCurio.registerAll();
        event.enqueueWork(() -> {
            com.blessingofpandora.compat.love.EternalLoveMobility.registerTicketValidation();
            LoveOrbRegistration.initCompat();
            if (LoveOrbRegistration.isAvailable())
                com.blessingofpandora.compat.love.LoveSharing.register();
        });
    }

    private static void modifyAttributes(EntityAttributeModificationEvent event)
    {
        if (LoveOrbRegistration.isAvailable())
            com.blessingofpandora.compat.love.LoveSharing.modifyAttributes(event);
        event.add(EntityType.PLAYER, CURSED_TO_BLESSED.get());
        for (EntityType<? extends LivingEntity> entityType : event.getTypes())
        {
            event.add(entityType, STRONG.get());
            event.add(entityType, SOUL_DAMAGE_TAKEN.get());
        }
    }
}
