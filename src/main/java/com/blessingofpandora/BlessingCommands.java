package com.blessingofpandora;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class BlessingCommands
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<String> SUGGESTIONS = new ArrayList<>();

    private BlessingCommands()
    {
    }

    static
    {
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            SUGGESTIONS.add(shortName(definition));
            SUGGESTIONS.add(definition.id());
        }
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        if (!BlessingOfPandoraConfig.commandsEnabled())
        {
            return;
        }

        event.getDispatcher().register(Commands.literal("blessingofpandora")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("number", IntegerArgumentType.integer(1, 7))
                        .executes(BlessingCommands::convertNumber))
                .then(Commands.literal("give")
                        .then(blessingArgument()))
                .then(giveAllCommand()));

        event.getDispatcher().register(Commands.literal("giveblessing")
                .requires(source -> source.hasPermission(2))
                .then(blessingArgument()));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> blessingArgument()
    {
        return Commands.argument("blessing", StringArgumentType.word())
                .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(SUGGESTIONS, builder))
                .executes(BlessingCommands::giveToSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(BlessingCommands::giveToPlayer));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> giveAllCommand()
    {
        return Commands.literal("giveall")
                .executes(BlessingCommands::giveAllToSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(BlessingCommands::giveAllToPlayer));
    }

    private static int giveToSelf(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        return give(context, context.getSource().getPlayerOrException());
    }

    private static int giveToPlayer(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        return give(context, EntityArgument.getPlayer(context, "player"));
    }

    private static int giveAllToSelf(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        return giveAll(context, context.getSource().getPlayerOrException());
    }

    private static int giveAllToPlayer(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        return giveAll(context, EntityArgument.getPlayer(context, "player"));
    }

    private static int convertNumber(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int number = IntegerArgumentType.getInteger(context, "number");
        BlessingDefinitions definition = BlessingDefinitions.values()[number - 1];
        if (!ReversalPermission.isAllowed(player, definition))
        {
            BlessingLogs.info(LOGGER, "[BlessingOfPandora] Command conversion blocked by permission: blessing={}, player={}",
                    definition.id(), player.getScoreboardName());
            context.getSource().sendFailure(Component.literal(
                    "This reversal is currently disabled."));
            return 0;
        }
        boolean converted = ReversalActions.convertWornCurse(player, definition);
        if (!converted)
        {
            BlessingLogs.info(LOGGER, "[BlessingOfPandora] Command conversion found no matching curse stack: blessing={}, player={}",
                    definition.id(), player.getScoreboardName());
            context.getSource().sendFailure(Component.literal(
                    "You are not wearing the matching curse."));
            return 0;
        }
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> context, ServerPlayer target)
    {
        BlessingDefinitions definition = resolveDefinition(
                StringArgumentType.getString(context, "blessing"));
        if (definition == null)
        {
            context.getSource().sendFailure(Component.literal(
                    "Unknown blessing: " + StringArgumentType.getString(context, "blessing")));
            return 0;
        }

        ItemStack stack = definition.createStack();
        if (stack.isEmpty())
        {
            context.getSource().sendFailure(
                    Component.literal("The curse item for this blessing is not available."));
            return 0;
        }

        giveStack(context.getSource(), target, stack);
        return 1;
    }

    private static int giveAll(CommandContext<CommandSourceStack> context, ServerPlayer target)
    {
        int count = 0;
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            ItemStack stack = definition.createStack();
            if (!stack.isEmpty())
            {
                giveStack(context.getSource(), target, stack);
                count++;
            }
        }
        return count;
    }

    private static void giveStack(CommandSourceStack source, ServerPlayer target, ItemStack stack)
    {
        if (!target.getInventory().add(stack))
        {
            target.drop(stack, false);
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.give.success.single",
                stack.getCount(),
                stack.getDisplayName(),
                target.getDisplayName()), true);
    }

    private static BlessingDefinitions resolveDefinition(String input)
    {
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            if (definition.id().equals(input) || shortName(definition).equals(input))
            {
                return definition;
            }
        }
        return null;
    }

    private static String shortName(BlessingDefinitions definition)
    {
        return definition.id().replace("blessing_of_", "");
    }
}
