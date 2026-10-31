/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/ and the bundled notice.
 * Component traversal adapted from The Long Travail's NameColoring,
 * commit f295c4ead89c0a99ef2f7ed9f56e9cbba42bfb34.
 */
package com.blessingofpandora;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Serializable display metadata only: no client classes, ticking or item-NBT mutation. */
public final class LoveNameStyle
{
    public static final String MARKER = "blessingofpandora:love_flow_name";
    public static final int FALLBACK_PINK = 0xE39AB5;

    private LoveNameStyle() {}

    public static Component decorate(Component source)
    {
        if (source == null) return Component.empty();
        var contents = source.getContents();
        if (contents instanceof TranslatableContents translation)
        {
            Object[] arguments = translation.getArgs().clone();
            for (int i = 0; i < arguments.length; i++)
                if (arguments[i] instanceof Component component) arguments[i] = decorate(component);
            contents = new TranslatableContents(translation.getKey(), translation.getFallback(), arguments);
        }
        MutableComponent result = MutableComponent.create(contents)
                .setStyle(source.getStyle().withColor(FALLBACK_PINK).withInsertion(MARKER));
        for (Component sibling : source.getSiblings()) result.append(decorate(sibling));
        return result;
    }
}
