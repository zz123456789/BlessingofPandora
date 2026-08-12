package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT)
public final class BlessingTooltip
{
    private BlessingTooltip()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        BlessingDefinitions definition = BlessingNbt.getBlessing(stack);
        if (definition == null)
        {
            if (PandoraBraceletItem.isPandoraBracelet(stack))
            {
                PandoraBraceletItem.addPandoraPoem(event.getToolTip(), stack);
                return;
            }
            Item prudence = BlessingDefinitions.REQUIEM.curseItem();
            if (prudence != null && stack.is(prudence) && !BlessingNbt.isBlessing(stack))
            {
                event.getToolTip().add(Component.translatable(
                                "item.blessingofpandora.effect.prudence_soul_sculpting")
                        .withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        List<Component> tooltip = event.getToolTip();
        UUID viewer = Minecraft.getInstance().player == null
                ? null
                : Minecraft.getInstance().player.getUUID();
        if (!BlessingOwners.isOwner(stack, viewer))
        {
            tooltip.clear();
            tooltip.add(Component.literal("????????????????????")
                    .withStyle(ChatFormatting.OBFUSCATED));
            tooltip.add(Component.literal("????????????????????")
                    .withStyle(ChatFormatting.OBFUSCATED));
            tooltip.add(Component.literal("????????????????????")
                    .withStyle(ChatFormatting.OBFUSCATED));
            tooltip.add(Component.translatable("item.blessingofpandora.tooltip.not_owner")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        if (!tooltip.isEmpty())
        {
            tooltip.set(0, stack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
        }
        if (tooltip.size() > 1 && isGoldSlotLine(tooltip.get(1)))
        {
            tooltip.subList(2, tooltip.size()).clear();
        }
        else if (tooltip.size() > 1)
        {
            tooltip.subList(1, tooltip.size()).clear();
        }

        addPoem(tooltip, definition);

        switch (definition)
        {
            case FREEDOM -> addFreedom(tooltip);
            case INFINITY -> addInfinity(tooltip);
            case SPIRIT -> addSpirit(tooltip);
            case STRENGTH -> addStrength(tooltip);
            case FEARLESSNESS -> addFearlessness(tooltip);
            case REQUIEM -> addRequiem(tooltip);
            case LIBERATION -> addLiberation(tooltip);
        }
    }

    private static void addFreedom(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.freedom_3");
        addBlue(tooltip, "item.blessingofpandora.effect.freedom_4");
    }

    private static void addInfinity(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.infinity_3");
        addBlue(tooltip, "item.blessingofpandora.effect.infinity_4");
    }

    private static void addSpirit(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.spirit_luck");
        addBlue(tooltip, "item.blessingofpandora.effect.spirit_3");
    }

    private static void addStrength(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.strength_strong");
        addBlue(tooltip, "item.blessingofpandora.effect.strength_3");
    }

    private static void addFearlessness(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.fearlessness_3");
    }

    private static void addRequiem(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.requiem_3");
        addBlue(tooltip, "item.blessingofpandora.effect.requiem_4");
    }

    private static void addLiberation(List<Component> tooltip)
    {
        addBlue(tooltip, "item.blessingofpandora.effect.liberation_2");
        addBlue(tooltip, "item.blessingofpandora.effect.liberation_3");
    }

    private static void addBlue(List<Component> tooltip, String key)
    {
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.BLUE));
    }

    private static void addPoem(List<Component> tooltip, BlessingDefinitions definition)
    {
        String[] lines = poemLines(definition);
        int index = Math.min(1, tooltip.size());
        TextColor silver = TextColor.fromRgb(0xC8D8E8);
        for (int i = 0; i < lines.length; i++)
        {
            if (i == 0)
            {
                tooltip.add(index++, Component.literal(lines[i])
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(ChatFormatting.BOLD));
            }
            else
            {
                tooltip.add(index++, Component.literal(lines[i])
                        .withStyle(style -> style.withColor(silver)));
            }
        }
        tooltip.add(index, Component.literal("✦ ──────── ✦")
                .withStyle(ChatFormatting.GOLD));
    }

    private static String[] poemLines(BlessingDefinitions definition)
    {
        switch (definition)
        {
            case FREEDOM:
                return new String[] {
                        "✦ 其一 · 囚笼 · 化圣殿 ✦",
                        "颂赞这四面坚墙，这围拢的圣火！",
                        "若非此界将流荡的星尘截停，",
                        "魂魄必散作太初混沌的残沫。",
                        "这笼壁是造物主的掌纹与宫壁，",
                        "让迷途的电流寻得归家的轨迹，",
                        "于方寸间，窥见无尽旋转的奥秘。"
                };
            case INFINITY:
                return new String[] {
                        "✦ 其二 · 制限 · 化律法 ✦",
                        "称颂这捆缚双翼的黄金准绳！",
                        "洪流若无堤岸，岂能倒映天庭？",
                        "飞鸟若无引力，何从丈量苍穹？",
                        "此限乃神明挥就的曲谱与诫命，",
                        "于无垠虚空中划出神圣的圆形，",
                        "教漫游的脚步，踏准恒星的行营。"
                };
            case SPIRIT:
                return new String[] {
                        "✦ 其三 · 血肉 · 化乐器 ✦",
                        "荣耀这易朽之躯，这尘泥的器皿！",
                        "灵魂若无剑鞘，必被狂风吹钝；",
                        "唯此温热的血肉能传导救赎的颤音。",
                        "它是竖琴，绷紧苦难与欢愉的弦，",
                        "在肋骨间震颤，奏出人间圣咏，",
                        "纵使弦断归尘，亦已鸣响过永恒。"
                };
            case STRENGTH:
                return new String[] {
                        "✦ 其四 · 羸弱 · 化谦卑 ✦",
                        "礼赞这芦苇之茎，这微末的握力！",
                        "顽石虽硬，终被潮水磨作空壳；",
                        "柔枝虽弯，却承露珠而不自诩。",
                        "此弱乃深渊边缘搭起的窄梯，",
                        "教高昂的头颅懂得俯首的奥义，",
                        "惟有虚空之手，能接住倾泻的甘霖。"
                };
            case FEARLESSNESS:
                return new String[] {
                        "✦ 其五 · 恐惧 · 化警醒 ✦",
                        "敬畏这刺骨寒风，这战栗的馈赠！",
                        "盲者无畏，必堕入无星的裂谷；",
                        "勇者知惧，方紧握船舵与火种。",
                        "如阿喀琉斯足踵那致命的疼痛，",
                        "此惧乃悬于额前的明镜与雾灯，",
                        "敛去妄光，只为照亮脚下方寸的路径。"
                };
            case REQUIEM:
                return new String[] {
                        "✦ 其六 · 碎魂 · 化虹光 ✦",
                        "歌咏这裂开的铜镜，这散落的玉！",
                        "浑圆之球虽美，却只映一副面孔；",
                        "碎成千片残星，反折射万重霞涌。",
                        "灵若不曾崩解，何来隙罅接纳光洪？",
                        "每一道碎痕皆是通往天穹的棱窗，",
                        "将惨白的虚无，切作七彩的救赎长虹。"
                };
            case LIBERATION:
            default:
                return new String[] {
                        "✦ 其七 · 咒缚 · 化天轭 ✦",
                        "高举这沉重锁链，这神圣的镣铐！",
                        "普罗米修斯被钉于高加索之巅，",
                        "反听见大地深处岩浆奔涌的心跳；",
                        "但丁沉入地狱最底的那层寒沼，",
                        "正因背负罪孽的铅砣与铁锚，",
                        "才得以借其重量，攀上星辰的弯角。"
                };
        }
    }

    private static boolean isGoldSlotLine(Component component)
    {
        TextColor gold = TextColor.fromLegacyFormat(ChatFormatting.GOLD);
        return gold != null && gold.equals(component.getStyle().getColor());
    }
}
