package com.blessingofpandora;

import org.slf4j.Logger;

public final class BlessingLogs
{
    private BlessingLogs()
    {
    }

    public static boolean enabled()
    {
        return BlessingOfPandoraConfig.detailedLogsEnabled();
    }

    public static void info(
            Logger logger, String message, Object... arguments)
    {
        if (enabled())
        {
            logger.info(message, arguments);
        }
    }

    public static void warn(
            Logger logger, String message, Object... arguments)
    {
        if (enabled())
        {
            logger.warn(message, arguments);
        }
    }
}
