package com.blessingofpandora.compat.love;

import java.text.Normalizer;
import java.util.List;

/** Literal vocabulary rule requested for the vow; negative wording takes precedence. */
public final class LoveVow
{
    public enum Result { NONE, PROMISE, BETRAYAL }
    public static final List<String> FORBIDDEN = List.of("不", "讨厌", "恨", "厌恶", "憎恶", "嫌弃",
            "恶心", "去死", "滚", "讨嫌", "烦人", "垃圾", "贱");
    private LoveVow() {}

    public static Result classify(String message)
    {
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFKC);
        StringBuilder plain = new StringBuilder(normalized.length());
        normalized.codePoints().filter(c -> !Character.isWhitespace(c)
                && Character.getType(c) != Character.FORMAT).forEach(plain::appendCodePoint);
        String text = plain.toString();
        if (!text.contains("女仆")) return Result.NONE;
        for (String word : FORBIDDEN) if (text.contains(word)) return Result.BETRAYAL;
        return text.contains("我爱你") ? Result.PROMISE : Result.NONE;
    }
}
