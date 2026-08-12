package com.blessingofpandora;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(BlessingOfPandora.MODID)
public class BlessingOfPandora
{
    public static final String MODID = "blessingofpandora";

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MODID);
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
    public static final RegistryObject<MobEffect> SOUL_SCULPTING =
            MOB_EFFECTS.register("soul_sculpting",
                    () -> new SoulSculptingEffect(MobEffectCategory.BENEFICIAL, 0x88E8FF));

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
                    })
                    .build());

    public BlessingOfPandora(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        MOB_EFFECTS.register(modEventBus);
        ATTRIBUTES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(BlessingOfPandora::commonSetup);
        modEventBus.addListener(BlessingOfPandora::modifyAttributes);
    }

    private static void commonSetup(FMLCommonSetupEvent event)
    {
        BlessingCurio.registerAll();
    }

    private static void modifyAttributes(EntityAttributeModificationEvent event)
    {
        event.add(EntityType.PLAYER, CURSED_TO_BLESSED.get());
        for (EntityType<? extends LivingEntity> entityType : event.getTypes())
        {
            event.add(entityType, STRONG.get());
        }
    }
}
