package com.blessingofpandora.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

/** Draws only this mod's advancement links; the original chat contents and interactions stay intact. */
public final class BlessingAdvancementChatStyle
{
    private static final String PREFIX = "advancements.blessingofpandora.";
    private static final Set<String> TITLES = Set.of(
            PREFIX + "root.title", PREFIX + "freedom.title", PREFIX + "infinity.title",
            PREFIX + "spirit.title", PREFIX + "strength.title", PREFIX + "fearlessness.title",
            PREFIX + "requiem.title", PREFIX + "liberation.title");
    private static final int MAX_CACHED_LINES = 256;
    private final Map<FormattedCharSequence, List<Run>> lines = new LinkedHashMap<>(32, 0.75F, true);
    private long resourceRevision = Long.MIN_VALUE;
    private Font currentFont;

    public int drawLine(GuiGraphics graphics, Font font, FormattedCharSequence line,
                        int x, int y, int color)
    {
        long revision = ReversalUiResources.revision();
        if (revision != resourceRevision || font != currentFont)
        {
            lines.clear();
            resourceRevision = revision;
            currentFont = font;
        }
        List<Run> runs = lines.get(line);
        if (runs == null)
        {
            runs = splitLine(line, font);
            if (lines.size() >= MAX_CACHED_LINES) lines.remove(lines.keySet().iterator().next());
            lines.put(line, runs);
        }
        if (runs.isEmpty()) return graphics.drawString(font, line, x, y, color);

        float cursor = x;
        for (Run run : runs)
        {
            if (run.animated())
                BlessingAdvancementStyle.drawChatTitle(graphics, font, run.text(), cursor, y, color);
            else
                graphics.drawString(font, run.text(), cursor, (float) y, color, true);
            // Static run widths retain fractional advances and are refreshed on font/resource changes.
            cursor += run.width();
        }
        return (int) cursor + 1;
    }

    private static List<Run> splitLine(FormattedCharSequence line, Font font)
    {
        Map<HoverEvent, Boolean> hovers = new IdentityHashMap<>();
        boolean[] found = {false};
        line.accept((index, style, codePoint) -> {
            found[0] = isTitle(style, hovers);
            return !found[0];
        });
        // Negative results are cached too: ordinary chat is not re-inspected every frame.
        if (!found[0]) return List.of();

        List<Run> runs = new ArrayList<>();
        List<FormattedCharSequence> glyphs = new ArrayList<>();
        boolean[] animated = {false};
        line.accept((index, style, codePoint) -> {
            boolean nextAnimated = isTitle(style, hovers);
            if (!glyphs.isEmpty() && nextAnimated != animated[0])
            {
                runs.add(run(glyphs, animated[0], font));
                glyphs.clear();
            }
            animated[0] = nextAnimated;
            glyphs.add(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });
        if (!glyphs.isEmpty())
            runs.add(run(glyphs, animated[0], font));
        return List.copyOf(runs);
    }

    private static Run run(List<FormattedCharSequence> glyphs, boolean animated, Font font)
    {
        FormattedCharSequence text = FormattedCharSequence.composite(List.copyOf(glyphs));
        return new Run(text, animated, font.getSplitter().stringWidth(text));
    }

    private static boolean isTitle(Style style, Map<HoverEvent, Boolean> hovers)
    {
        HoverEvent hover = style.getHoverEvent();
        if (hover == null) return false;
        return hovers.computeIfAbsent(hover, BlessingAdvancementChatStyle::isAdvancementHover);
    }

    private static boolean isAdvancementHover(HoverEvent hover)
    {
        Component text = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        // Vanilla puts the translated title at the root of the advancement hover text.
        // Match its matching description too; a literal with the same visible name is not a link.
        if (text == null || !(text.getContents() instanceof TranslatableContents translated)
                || !TITLES.contains(translated.getKey())) return false;
        String descriptionKey = translated.getKey().substring(0, translated.getKey().length() - 6)
                + ".description";
        return containsTranslation(text, descriptionKey, 0);
    }

    private static boolean containsTranslation(Component component, String key, int depth)
    {
        if (depth > 16) return false;
        if (component.getContents() instanceof TranslatableContents translated)
        {
            if (key.equals(translated.getKey())) return true;
            for (Object argument : translated.getArgs())
                if (argument instanceof Component child && containsTranslation(child, key, depth + 1)) return true;
        }
        for (Component child : component.getSiblings())
            if (containsTranslation(child, key, depth + 1)) return true;
        return false;
    }

    private record Run(FormattedCharSequence text, boolean animated, float width) {}
}
