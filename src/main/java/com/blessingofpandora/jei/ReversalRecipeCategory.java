package com.blessingofpandora.jei;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.BlessingNbt;
import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.ReversalDescriptions;
import com.blessingofpandora.client.ReversalLiveData;
import com.blessingofpandora.client.ReversalTextLayoutCache;
import com.blessingofpandora.client.ReversalUiResources;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Uses JEI's public scrolling API without freezing live text at recipe registration. */
public final class ReversalRecipeCategory implements IRecipeCategory<BlessingDefinitions>
{
    public static final RecipeType<BlessingDefinitions> TYPE = RecipeType.create(
            BlessingOfPandora.MODID, "reversal", BlessingDefinitions.class);
    // Match JEI's native ingredient-information width, including its scrollbar.
    private static final int WIDTH = 170;
    private static final int HEIGHT = 150;
    private final IDrawable icon;

    public ReversalRecipeCategory(IGuiHelper guiHelper)
    {
        Item curse = BlessingDefinitions.FREEDOM.curseItem();
        icon = guiHelper.createDrawableItemStack(new ItemStack(curse == null ? Items.ENDER_EYE : curse));
    }

    @Override
    public RecipeType<BlessingDefinitions> getRecipeType()
    {
        return TYPE;
    }

    @Override
    public Component getTitle()
    {
        return Component.translatable("jei.blessingofpandora.reversal.title");
    }

    @Override
    public int getWidth()
    {
        return WIDTH;
    }

    @Override
    public int getHeight()
    {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlessingDefinitions definition, IFocusGroup focuses)
    {
        Item curse = definition.curseItem();
        if (curse == null) return;
        List<ItemStack> ingredients = List.of(new ItemStack(curse), BlessingNbt.createStack(definition));
        builder.addInputSlot((WIDTH - 16) / 2, 1).setStandardSlotBackground().addItemStacks(ingredients);
        // Preserve both recipe and usage lookup for the curse and its blessing form.
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(ingredients);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BlessingDefinitions definition,
            IFocusGroup focuses)
    {
        var scroll = builder.addScrollBoxWidget(WIDTH, HEIGHT - 24, 0, 24);
        scroll.setContents(createDescriptionDrawable(definition, scroll.getContentAreaWidth()));
    }

    public static IDrawable createDescriptionDrawable(BlessingDefinitions definition, int width)
    {
        return new LiveDescription(definition, width);
    }

    /** Resolves every value live so an open recipe follows server progress and config changes. */
    public static Component description(BlessingDefinitions definition)
    {
        MutableComponent description = ReversalDescriptions.title(definition).copy();
        for (Component step : ReversalDescriptions.steps(definition))
        {
            description.append("\n").append(step);
        }
        Component live = definition == BlessingDefinitions.REQUIEM
                ? ReversalLiveData.liveRequiemStep()
                : ReversalLiveData.liveStateStep(definition);
        if (!live.getString().isEmpty()) description.append("\n").append(live);
        if (definition == BlessingDefinitions.LIBERATION)
        {
            description.append("\n").append(ReversalLiveData.liveLiberationStep());
        }
        return description.append("\n").append(ReversalDescriptions.result(definition));
    }

    private static final class LiveDescription implements IDrawable
    {
        private static final int PADDING = 4;
        private static final int CONDITION_INDENT = 12;
        private static final TextColor GOLD = TextColor.fromRgb(0x795414);
        private static final TextColor TEAL = TextColor.fromRgb(0x006775);
        private static final TextColor GREEN = TextColor.fromRgb(0x286238);
        private static final TextColor GRAY = TextColor.fromRgb(0x555555);
        private final BlessingDefinitions definition;
        private final int width;
        private final ReversalTextLayoutCache<TextLine> layout = new ReversalTextLayoutCache<>();

        private LiveDescription(BlessingDefinitions definition, int width)
        {
            this.definition = definition;
            this.width = Math.max(1, width);
        }

        @Override
        public int getWidth()
        {
            return width;
        }

        @Override
        public int getHeight()
        {
            List<TextLine> lines = lines();
            return lines.isEmpty() ? PADDING * 2
                    : lines.get(lines.size() - 1).y() + Minecraft.getInstance().font.lineHeight + PADDING;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset)
        {
            var font = Minecraft.getInstance().font;
            for (TextLine line : lines())
            {
                if (line.divider())
                {
                    graphics.hLine(xOffset + PADDING, xOffset + width - PADDING - 1,
                            yOffset + line.y() - 5, 0xFF9B9B9B);
                }
                graphics.drawString(font, line.text(), xOffset + line.x(), yOffset + line.y(),
                        0xFF303030, false);
            }
        }

        private List<TextLine> lines()
        {
            Component current = description(definition);
            var font = Minecraft.getInstance().font;
            return layout.lines(current, width, font, Language.getInstance(), ReversalUiResources.revision(),
                    () -> arrange(font));
        }

        private List<TextLine> arrange(Font font)
        {
            List<TextLine> lines = new ArrayList<>();
            int y = addParagraph(lines, font, Component.translatable(definition.nameKey()),
                    PADDING, PADDING, "", Tone.TITLE, false) + 4;
            int number = 1;
            List<Component> steps = ReversalDescriptions.steps(definition);
            for (int i = 0; i < steps.size(); i++)
            {
                boolean note = definition == BlessingDefinitions.REQUIEM && i == 2;
                y = addParagraph(lines, font, steps.get(i), PADDING + CONDITION_INDENT, y,
                        note ? "" : number++ + ".", note ? Tone.NOTE : Tone.BODY, false) + 3;
            }

            Component live = definition == BlessingDefinitions.REQUIEM
                    ? ReversalLiveData.liveRequiemStep() : ReversalLiveData.liveStateStep(definition);
            if (!live.getString().isEmpty() || definition == BlessingDefinitions.LIBERATION)
            {
                y = addParagraph(lines, font, Component.translatable("jei.blessingofpandora.reversal.state"),
                        PADDING, y + 5, "", Tone.HEADING, true) + 2;
                if (!live.getString().isEmpty())
                {
                    y = addParagraph(lines, font, live, PADDING, y, "", Tone.BODY, false) + 3;
                }
                if (definition == BlessingDefinitions.LIBERATION)
                {
                    y = addParagraph(lines, font, ReversalLiveData.liveLiberationStep(),
                            PADDING, y, "", Tone.BODY, false) + 3;
                }
            }
            addParagraph(lines, font, ReversalDescriptions.result(definition),
                    PADDING, y + 5, "", Tone.RESULT, true);
            return lines;
        }

        private int addParagraph(List<TextLine> lines, Font font, Component text, int indent, int y,
                String marker, Tone tone, boolean divider)
        {
            // Width belongs to JEI's content area, never the whole widget or the string length.
            int x = Math.min(indent, Math.max(0, width - PADDING - 1));
            int wrapWidth = Math.max(1, width - x - PADDING);
            if (!marker.isEmpty())
            {
                lines.add(new TextLine(Component.literal(marker).withStyle(ChatFormatting.DARK_GRAY)
                        .getVisualOrderText(), PADDING, y, false));
            }
            boolean first = true;
            for (FormattedCharSequence line : font.split(present(text, tone), wrapWidth))
            {
                lines.add(new TextLine(line, x, y, divider && first));
                first = false;
                y += font.lineHeight + 2;
            }
            return y;
        }

        private static Component present(Component source, Tone tone)
        {
            List<TextRun> runs = new ArrayList<>();
            boolean[] first = {true};
            source.visit((style, value) -> {
                if (value.isEmpty()) return Optional.empty();
                if (first[0])
                {
                    // Replace the shared tooltip bullet with our own hanging step number.
                    if (value.startsWith("· ")) value = value.substring(2);
                    first[0] = false;
                }
                Style shown = style.withItalic(false);
                TextColor color = style.getColor();
                if (tone == Tone.TITLE || tone == Tone.RESULT) shown = shown.withColor(GOLD).withBold(true);
                else if (tone == Tone.HEADING) shown = shown.withColor(GRAY).withBold(true);
                else if (tone == Tone.NOTE) shown = shown.withColor(GRAY);
                else if (TextColor.fromLegacyFormat(ChatFormatting.YELLOW).equals(color)) shown = shown.withColor(GOLD);
                else if (TextColor.fromLegacyFormat(ChatFormatting.AQUA).equals(color)) shown = shown.withColor(TEAL);
                else if (TextColor.fromLegacyFormat(ChatFormatting.GREEN).equals(color)) shown = shown.withColor(GREEN);
                else if (TextColor.fromLegacyFormat(ChatFormatting.GRAY).equals(color)) shown = shown.withColor(GRAY);
                runs.add(new TextRun(value, shown));
                return Optional.empty();
            }, Style.EMPTY);

            MutableComponent result = Component.empty();
            boolean boundary = true;
            for (int i = 0; i < runs.size(); i++)
            {
                TextRun run = runs.get(i);
                String value = run.text();
                if (value.isEmpty()) continue;
                if (isNumber(value))
                {
                    // Chinese has no spaces around values. Give Font.split word boundaries
                    // so a fitting XP total, threshold or decimal moves together to the next line.
                    boolean coordinate = i > 0 && (runs.get(i - 1).text().endsWith("Y≥")
                            || runs.get(i - 1).text().endsWith("Y≤"));
                    if (!boundary && !coordinate) result.append(" ");
                    result.append(Component.literal(value).setStyle(run.style()));
                    for (int next = i + 1; next < runs.size(); next++)
                    {
                        TextRun suffix = runs.get(next);
                        if (suffix.text().isEmpty()) continue;
                        int end = 0;
                        // Percent signs and closing punctuation belong with the value, never
                        // at the start of the following line (e.g. 20%; or 0.0001%；).
                        while (end < suffix.text().length()
                                && "%，。；：！？、,.;:!?)]}）】》」』".indexOf(suffix.text().charAt(end)) >= 0) end++;
                        if (end == 0) break;
                        result.append(Component.literal(suffix.text().substring(0, end)).setStyle(suffix.style()));
                        runs.set(next, new TextRun(suffix.text().substring(end), suffix.style()));
                        // A translated '%' may be a separate run from its following punctuation.
                        if (end < suffix.text().length()) break;
                    }
                    result.append(" ");
                    boundary = true;
                }
                else
                {
                    if (boundary && value.startsWith(" ")) value = value.substring(1);
                    if (value.isEmpty()) continue;
                    result.append(Component.literal(value).setStyle(run.style()));
                    boundary = Character.isWhitespace(value.charAt(value.length() - 1));
                }
            }
            return result;
        }

        private static boolean isNumber(String text)
        {
            boolean digit = false;
            for (int i = 0; i < text.length(); i++)
            {
                char c = text.charAt(i);
                if (c >= '0' && c <= '9') digit = true;
                else if (c != '.' && !(i == 0 && c == '-')) return false;
            }
            return digit;
        }

        private enum Tone { TITLE, HEADING, BODY, NOTE, RESULT }

        private record TextRun(String text, Style style) {}

        private record TextLine(FormattedCharSequence text, int x, int y, boolean divider) {}
    }
}
