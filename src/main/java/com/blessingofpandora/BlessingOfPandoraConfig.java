package com.blessingofpandora;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(
        modid = BlessingOfPandora.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BlessingOfPandoraConfig
{
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DETAILED_LOGS;
    public static final ForgeConfigSpec.BooleanValue GIVE_STARTING_NECKLACE;
    public static final ForgeConfigSpec.BooleanValue GIVE_STARTING_CURSES;

    public static final ForgeConfigSpec.ConfigValue<String> FIRST_MOB;
    public static final ForgeConfigSpec.DoubleValue FIRST_BASE_CHANCE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue FIRST_FAILURE_BONUS_PERCENT;

    public static final ForgeConfigSpec.IntValue SECOND_START_Y;
    public static final ForgeConfigSpec.IntValue SECOND_TARGET_Y;

    public static final ForgeConfigSpec.ConfigValue<String> THIRD_EFFECT;

    public static final ForgeConfigSpec.DoubleValue FOURTH_MAX_HEALTH;
    public static final ForgeConfigSpec.ConfigValue<String> FOURTH_EFFECT;
    public static final ForgeConfigSpec.ConfigValue<String> FOURTH_MOB;

    public static final ForgeConfigSpec.ConfigValue<String> FIFTH_MOB;
    public static final ForgeConfigSpec.DoubleValue FIFTH_BASE_CHANCE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue FIFTH_FAILURE_BONUS_PERCENT;

    public static final ForgeConfigSpec.IntValue SIXTH_EXPERIENCE;

    public static final ForgeConfigSpec.DoubleValue SEVENTH_BASE_CHANCE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue SEVENTH_CURSE_BONUS_PERCENT;
    public static final ForgeConfigSpec.BooleanValue SEVENTH_CLEAR_CURSE_ENCHANTMENTS;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        GIVE_STARTING_NECKLACE = builder
                .comment("首次进入世界是否赠送潘多拉项链；默认 true，false 时若开启赠送诅咒，则将七个诅咒物品直接给予玩家；不影响已领取的物品。")
                .define("giveStartingNecklace", true);
        GIVE_STARTING_CURSES = builder
                .comment("首次进入世界是否赠送七个现实诅咒；默认 true，有项链时放入项链，无项链时直接给予玩家；false 时项链为空，两个开关均为 false 时均不赠送。")
                .define("giveStartingCurses", true);
        ENABLE_COMMANDS = builder
                .comment("是否启用 /blessingofpandora 和 /giveblessing 管理员指令；默认 false，修改后需重启。")
                .define("enableCommands", false);
        ENABLE_DETAILED_LOGS = builder
                .comment("是否输出潘多拉之祝的详细调试日志；默认 false。")
                .define("enableDetailedLogs", false);

        builder.comment("现实第一诅咒：囚笼 → 理想第一祝福：自由")
                .push("reversal_first");
        FIRST_MOB = builder
                .comment("玩家或其仆从需要击杀的生物注册 ID（如 minecraft:wither）；填写 any 表示任意生物，玩家仍需佩戴对应诅咒并允许自动反转。")
                .define("mob", "minecraft:wither");
        FIRST_BASE_CHANCE_PERCENT = builder
                .comment("佩戴第一诅咒且允许自动反转时，每次击杀目标的基础转化概率，单位为百分比；默认 20，即 20%。")
                .defineInRange("baseChancePercent", 20.0, 0.0, 100.0);
        FIRST_FAILURE_BONUS_PERCENT = builder
                .comment("每次符合条件但未转化的击杀，为后续击杀增加的概率，单位为百分点；默认 10，最高累计至 100%；失败次数在卸下、重登和死亡后保留，成功转化后重置。")
                .defineInRange("failureBonusPercent", 10.0, 0.0, 100.0);
        builder.pop();

        builder.comment("现实第二诅咒：制限 → 理想第二祝福：无限")
                .push("reversal_second");
        SECOND_START_Y = builder
                .comment("在主世界开始下落的最低 Y 坐标；默认 666。")
                .defineInRange("startY", 666, -4096, 4096);
        SECOND_TARGET_Y = builder
                .comment("下落必须到达的 Y 坐标或更低位置；默认 -60。")
                .defineInRange("targetY", -60, -4096, 4096);
        builder.pop();

        builder.comment("现实第三诅咒：血肉 → 理想第三祝福：精神")
                .push("reversal_third");
        THIRD_EFFECT = builder
                .comment("被闪电击中时必须拥有的药水效果注册 ID；默认 minecraft:invisibility（隐身）。")
                .define("effect", "minecraft:invisibility");
        builder.pop();

        builder.comment("现实第四诅咒：羸弱 → 理想第四祝福：强壮")
                .push("reversal_fourth");
        FOURTH_MAX_HEALTH = builder
                .comment("被击杀生物的最低生命上限，单位为生命值（2 点 = 1 颗心）；默认 325。")
                .defineInRange("maxHealth", 325.0, 1.0, 1000000.0);
        FOURTH_EFFECT = builder
                .comment("被击杀生物必须拥有的药水效果注册 ID；默认 minecraft:weakness（虚弱）。")
                .define("effect", "minecraft:weakness");
        FOURTH_MOB = builder
                .comment("玩家或其仆从需要击杀的生物注册 ID（如 minecraft:wither）；填写 any 表示任意生物，玩家仍需佩戴对应诅咒并允许自动反转。")
                .define("mob", "any");
        builder.pop();

        builder.comment("现实第五诅咒：恐惧 → 理想第五祝福：无畏")
                .push("reversal_fifth");
        FIFTH_MOB = builder
                .comment("玩家或其仆从需要击杀的生物注册 ID（如 minecraft:warden）；填写 any 表示任意生物，玩家仍需佩戴对应诅咒并允许自动反转。")
                .define("mob", "minecraft:warden");
        FIFTH_BASE_CHANCE_PERCENT = builder
                .comment("佩戴第五诅咒且允许自动反转时，每次击杀目标的基础转化概率，单位为百分比；默认 20，即 20%。")
                .defineInRange("baseChancePercent", 20.0, 0.0, 100.0);
        FIFTH_FAILURE_BONUS_PERCENT = builder
                .comment("每次符合条件但未转化的击杀，为后续击杀增加的概率，单位为百分点；默认 10，最高累计至 100%；失败次数在卸下、重登和死亡后保留，成功转化后重置。")
                .defineInRange("failureBonusPercent", 10.0, 0.0, 100.0);
        builder.pop();

        builder.comment("现实第六诅咒：碎魂 → 理想第六祝福：安魂")
                .push("reversal_sixth");
        SIXTH_EXPERIENCE = builder
                .comment("拥有塑魂效果时需要累计吸收的经验点数（不是经验等级）；默认 2000000。")
                .defineInRange("experience", 2_000_000, 1, 1_000_000_000);
        builder.pop();

        builder.comment("现实第七诅咒：咒缚 → 理想第七祝福：解脱")
                .push("reversal_seventh");
        SEVENTH_BASE_CHANCE_PERCENT = builder
                .comment("每游戏刻的基础转化概率，单位为百分比（正常每秒 20 刻）；默认 0.0001，即 0.0001%。")
                .defineInRange("baseChancePercent", 0.0001, 0.0, 100.0);
        SEVENTH_CURSE_BONUS_PERCENT = builder
                .comment("每个诅咒附魔提供的每游戏刻额外转化概率，单位为百分比；默认 0.0005，即增加 0.0005%。")
                .defineInRange("curseBonusPercent", 0.0005, 0.0, 100.0);
        SEVENTH_CLEAR_CURSE_ENCHANTMENTS = builder
                .comment("第七诅咒转化为祝福时是否清除该物品上的诅咒附魔（包含潘多拉诅咒）；默认 true，false 时保留全部附魔；不影响普通附魔及其他装备。")
                .define("clearCurseEnchantmentsOnConversion", true);
        builder.pop();

        SPEC = builder.build();
    }

    private BlessingOfPandoraConfig()
    {
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC)
        {
            return;
        }
        if (event instanceof ModConfigEvent.Reloading)
        {
            net.minecraft.server.MinecraftServer server =
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null)
            {
                server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> {
                    com.blessingofpandora.network.BlessingNetwork.syncConfig(player);
                    BlessingReversals.syncReversalState(player);
                }));
            }
        }
    }

    public static boolean commandsEnabled()
    {
        return ENABLE_COMMANDS.get();
    }

    public static boolean detailedLogsEnabled()
    {
        return ENABLE_DETAILED_LOGS.get();
    }

    public static String firstMob()
    {
        return FIRST_MOB.get();
    }

    public static double firstBaseChancePercent()
    {
        return FIRST_BASE_CHANCE_PERCENT.get();
    }

    public static double firstFailureBonusPercent()
    {
        return FIRST_FAILURE_BONUS_PERCENT.get();
    }

    public static int secondStartY()
    {
        return SECOND_START_Y.get();
    }

    public static int secondTargetY()
    {
        return SECOND_TARGET_Y.get();
    }

    public static String thirdEffect()
    {
        return THIRD_EFFECT.get();
    }

    public static double fourthMaxHealth()
    {
        return FOURTH_MAX_HEALTH.get();
    }

    public static String fourthEffect()
    {
        return FOURTH_EFFECT.get();
    }

    public static String fourthMob()
    {
        return FOURTH_MOB.get();
    }

    public static double fifthBaseChancePercent()
    {
        return FIFTH_BASE_CHANCE_PERCENT.get();
    }

    public static double fifthFailureBonusPercent()
    {
        return FIFTH_FAILURE_BONUS_PERCENT.get();
    }

    public static String fifthMob()
    {
        return FIFTH_MOB.get();
    }

    public static int sixthExperience()
    {
        return SIXTH_EXPERIENCE.get();
    }

    public static double seventhBaseChancePercent()
    {
        return SEVENTH_BASE_CHANCE_PERCENT.get();
    }

    public static double seventhCurseBonusPercent()
    {
        return SEVENTH_CURSE_BONUS_PERCENT.get();
    }

    public static double seventhBaseChance()
    {
        return SEVENTH_BASE_CHANCE_PERCENT.get() / 100.0;
    }

    public static double seventhCurseBonus()
    {
        return SEVENTH_CURSE_BONUS_PERCENT.get() / 100.0;
    }
}
