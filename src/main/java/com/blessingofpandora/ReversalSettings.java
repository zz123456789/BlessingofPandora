package com.blessingofpandora;

import net.minecraft.network.FriendlyByteBuf;

/** An immutable server snapshot used only by presentation code. */
public record ReversalSettings(
        String firstMob,
        double firstBaseChancePercent,
        double firstFailureBonusPercent,
        int secondStartY,
        int secondTargetY,
        String thirdEffect,
        double fourthMaxHealth,
        String fourthEffect,
        String fourthMob,
        String fifthMob,
        double fifthBaseChancePercent,
        double fifthFailureBonusPercent,
        int sixthExperience,
        double seventhBaseChancePercent,
        double seventhCurseBonusPercent)
{
    private static volatile ReversalSettings serverSettings;

    public static ReversalSettings capture()
    {
        return new ReversalSettings(
                BlessingOfPandoraConfig.firstMob(),
                BlessingOfPandoraConfig.firstBaseChancePercent(),
                BlessingOfPandoraConfig.firstFailureBonusPercent(),
                BlessingOfPandoraConfig.secondStartY(),
                BlessingOfPandoraConfig.secondTargetY(),
                BlessingOfPandoraConfig.thirdEffect(),
                BlessingOfPandoraConfig.fourthMaxHealth(),
                BlessingOfPandoraConfig.fourthEffect(),
                BlessingOfPandoraConfig.fourthMob(),
                BlessingOfPandoraConfig.fifthMob(),
                BlessingOfPandoraConfig.fifthBaseChancePercent(),
                BlessingOfPandoraConfig.fifthFailureBonusPercent(),
                BlessingOfPandoraConfig.sixthExperience(),
                BlessingOfPandoraConfig.seventhBaseChancePercent(),
                BlessingOfPandoraConfig.seventhCurseBonusPercent());
    }

    public static ReversalSettings display()
    {
        ReversalSettings value = serverSettings;
        return value == null ? capture() : value;
    }

    public static void setServerSettings(ReversalSettings value)
    {
        serverSettings = value;
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(firstMob, 256);
        buffer.writeDouble(firstBaseChancePercent);
        buffer.writeDouble(firstFailureBonusPercent);
        buffer.writeInt(secondStartY);
        buffer.writeInt(secondTargetY);
        buffer.writeUtf(thirdEffect, 256);
        buffer.writeDouble(fourthMaxHealth);
        buffer.writeUtf(fourthEffect, 256);
        buffer.writeUtf(fourthMob, 256);
        buffer.writeUtf(fifthMob, 256);
        buffer.writeDouble(fifthBaseChancePercent);
        buffer.writeDouble(fifthFailureBonusPercent);
        buffer.writeInt(sixthExperience);
        buffer.writeDouble(seventhBaseChancePercent);
        buffer.writeDouble(seventhCurseBonusPercent);
    }

    public static ReversalSettings decode(FriendlyByteBuf buffer)
    {
        return new ReversalSettings(
                buffer.readUtf(256),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(256),
                buffer.readDouble(),
                buffer.readUtf(256),
                buffer.readUtf(256),
                buffer.readUtf(256),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readDouble());
    }
}

