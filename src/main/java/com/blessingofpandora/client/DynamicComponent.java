package com.blessingofpandora.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class DynamicComponent implements Component
{
    private final Supplier<Component> supplier;
    private final Style style;
    private final boolean resolveOnce;

    public DynamicComponent(Supplier<Component> supplier)
    {
        this(supplier, Style.EMPTY);
    }

    public DynamicComponent(Supplier<Component> supplier, Style style)
    {
        this(supplier, style, false);
    }

    private DynamicComponent(Supplier<Component> supplier, Style style, boolean resolveOnce)
    {
        this.supplier = supplier;
        this.style = style;
        this.resolveOnce = resolveOnce;
    }

    /** Only for pure description suppliers: live equipment queries must use the normal constructor. */
    public static DynamicComponent description(Supplier<Component> supplier)
    {
        return new DynamicComponent(supplier, Style.EMPTY, true);
    }

    @Override
    public Style getStyle()
    {
        return style;
    }

    @Override
    public ComponentContents getContents()
    {
        return current().getContents();
    }

    @Override
    public List<Component> getSiblings()
    {
        return current().getSiblings();
    }

    @Override
    public FormattedCharSequence getVisualOrderText()
    {
        return current().getVisualOrderText();
    }

    @Override
    public String getString()
    {
        return current().getString();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor)
    {
        // Pandora equipment queries can update their containers on every read.
        if (!resolveOnce) return Component.super.visit(visitor);
        // Resolve once for a traversal, without caching between JEI/layout evaluations.
        return current().visit(visitor);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style parentStyle)
    {
        if (!resolveOnce) return Component.super.visit(visitor, parentStyle);
        Component value = current();
        // Match Component's default traversal: this wrapper supplies the root style.
        Style merged = style.applyTo(parentStyle);
        Optional<T> result = value.getContents().visit(visitor, merged);
        if (result.isPresent()) return result;
        for (Component sibling : value.getSiblings())
        {
            result = sibling.visit(visitor, merged);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    private Component current()
    {
        return supplier.get();
    }
}
