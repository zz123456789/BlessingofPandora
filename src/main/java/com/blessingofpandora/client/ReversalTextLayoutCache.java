package com.blessingofpandora.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Keeps only resolved text and layout output, never player/equipment state. */
public final class ReversalTextLayoutCache<T>
{
    private List<StyledRun> text;
    private Object font;
    private Object language;
    private long resourceRevision;
    private int width;
    private List<T> lines;
    private int visitedRuns;
    private List<StyledRun> changedText;
    private final FormattedText.StyledContentConsumer<Void> textVisitor = this::visitText;

    public List<T> lines(Component component, int width, Object font, Object language,
            long resourceRevision, Supplier<List<T>> split)
    {
        List<StyledRun> current = resolveText(component);
        if (lines == null || this.width != width || this.font != font
                || this.language != language || this.resourceRevision != resourceRevision
                || current != text)
        {
            lines = List.copyOf(split.get());
            text = current;
            this.width = width;
            this.font = font;
            this.language = language;
            this.resourceRevision = resourceRevision;
        }
        return lines;
    }

    /** Compare live text in one pass, allocating a new snapshot only when a run changes. */
    private List<StyledRun> resolveText(Component component)
    {
        visitedRuns = 0;
        changedText = text == null ? new ArrayList<>() : null;
        component.visit(textVisitor, Style.EMPTY);
        if (changedText == null && visitedRuns != text.size())
        {
            // A removed suffix also changes layout, even when every remaining run still matches.
            changedText = new ArrayList<>(text.subList(0, visitedRuns));
        }
        List<StyledRun> current = changedText == null ? text : changedText;
        changedText = null;
        return current;
    }

    private Optional<Void> visitText(Style style, String value)
    {
        if (changedText == null && (visitedRuns >= text.size()
                || !style.equals(text.get(visitedRuns).style())
                || !value.equals(text.get(visitedRuns).text())))
        {
            changedText = new ArrayList<>(Math.max(text.size(), visitedRuns + 1));
            for (int i = 0; i < visitedRuns; i++) changedText.add(text.get(i));
        }
        if (changedText != null) changedText.add(new StyledRun(style, value));
        visitedRuns++;
        return Optional.empty();
    }

    private record StyledRun(Style style, String text) {}
}
