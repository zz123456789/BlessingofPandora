package com.blessingofpandora.client;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.BlessingNbt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class ReversalHudScreenBase extends Screen
{
    private static final int MAX_PANEL_WIDTH = 560;
    private static final int MAX_PANEL_HEIGHT = 392;
    private static final int BUTTON_HEIGHT = 16;
    private static final int TITLE_Y = 12;
    private static final int CONTENT_TOP_OFFSET = 30;
    private static final int CONTENT_BOTTOM_OFFSET = 6;
    private static final Component ORNAMENT = Component.literal("✦");

    protected final BlessingDefinitions[] definitions;
    private final ItemStack[] icons;
    private final Component[] rowTitles;
    private final List<ReversalTextLayoutCache<FormattedCharSequence>> conditionLayouts;
    private long iconResourceRevision = -1;
    private int scrollOffset;

    protected ReversalHudScreenBase(
            Component title, BlessingDefinitions[] definitions)
    {
        super(title);
        this.definitions = definitions.clone();
        this.icons = new ItemStack[definitions.length];
        this.rowTitles = new Component[definitions.length];
        this.conditionLayouts = new ArrayList<>(definitions.length);
        for (int i = 0; i < definitions.length; i++)
        {
            rowTitles[i] = Component.literal((i + 1) + ". ")
                    .append(Component.translatable(definitions[i].nameKey()));
            conditionLayouts.add(new ReversalTextLayoutCache<>());
        }
    }

    protected abstract Component condition(int index);

    protected abstract Component buttonLabel(int index);

    protected abstract boolean buttonEnabled(int index);

    protected abstract void onButtonPressed(int index);

    @Override
    protected void init()
    {
        scrollOffset = 0;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics)
    {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 1)
        {
            this.onClose();
            return true;
        }
        if (button == 0)
        {
            Layout layout = layout();
            for (Row row : layout.rows)
            {
                if (!isFullyVisible(layout, row)
                        || !row.buttonEnabled
                        || !row.containsButton(mouseX, mouseY))
                {
                    continue;
                }
                onButtonPressed(row.index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        Layout layout = layout();
        if (layout.scrollMax <= 0)
        {
            return false;
        }
        int step = Math.max(8, layout.rowHeightHint / 2);
        scrollOffset = delta > 0.0
                ? Math.max(0, scrollOffset - step)
                : Math.min(layout.scrollMax, scrollOffset + step);
        return true;
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        Layout layout = layout();
        int right = layout.panelX + layout.panelWidth;
        int bottom = layout.panelY + layout.panelHeight;

        drawPanelFrame(graphics, layout, right, bottom);

        graphics.drawCenteredString(
                this.font,
                this.title,
                layout.panelX + layout.panelWidth / 2,
                layout.panelY + TITLE_Y,
                0xFFFFFFFF);

        for (Row row : layout.rows)
        {
            if (!isFullyVisible(layout, row))
            {
                continue;
            }
            drawRow(graphics, layout, row, mouseX, mouseY);
        }

        if (layout.scrollMax > 0)
        {
            drawScrollbar(graphics, layout);
        }
    }

    private void drawPanelFrame(
            GuiGraphics graphics, Layout layout, int right, int bottom)
    {
        graphics.fill(
                layout.panelX - 2, layout.panelY - 2,
                right + 2, bottom + 2,
                0xFF5E3D12);
        graphics.fill(
                layout.panelX - 1, layout.panelY - 1,
                right + 1, bottom + 1,
                0xFFD8A84C);
        graphics.fill(
                layout.panelX, layout.panelY,
                right, bottom,
                0xF20F1720);

        graphics.fill(
                layout.panelX + 2, layout.panelY + 2,
                right - 2, layout.panelY + 3,
                0x66332808);
        graphics.fill(
                layout.panelX + 2, bottom - 3,
                right - 2, bottom - 2,
                0x66332808);
        graphics.fill(
                layout.panelX + 2, layout.panelY + 3,
                layout.panelX + 3, bottom - 3,
                0x40332808);
        graphics.fill(
                right - 3, layout.panelY + 3,
                right - 2, bottom - 3,
                0x40332808);

        graphics.drawString(
                this.font, ORNAMENT,
                layout.panelX + 6, layout.panelY + 1,
                0xFFF1D27B);
        graphics.drawString(
                this.font, ORNAMENT,
                right - 13, layout.panelY + 1,
                0xFFF1D27B);
        graphics.drawString(
                this.font, ORNAMENT,
                layout.panelX + 6, bottom - 9,
                0xFFF1D27B);
        graphics.drawString(
                this.font, ORNAMENT,
                right - 13, bottom - 9,
                0xFFF1D27B);
    }

    private void drawRow(
            GuiGraphics graphics,
            Layout layout,
            Row row,
            int mouseX,
            int mouseY)
    {
        int rowY = row.screenY();
        int rowBottom = rowY + row.height;

        graphics.fill(
                layout.panelX + 10, rowBottom,
                layout.panelX + layout.panelWidth - 10,
                rowBottom + 1,
                0x28251B08);

        int contentHeight = 12
                + row.conditionLines.size() * 9;
        int contentY = rowY + Math.max(
                2, (row.height - contentHeight) / 2);

        if (layout.hasIconSpace)
        {
            ItemStack blessingStack = icon(row.index);
            if (!blessingStack.isEmpty())
            {
                graphics.renderItem(
                        blessingStack,
                        layout.panelX + 10,
                        rowY + (row.height - 16) / 2);
            }
        }

        graphics.drawString(
                this.font,
                rowTitles[row.index],
                layout.textX,
                contentY,
                0xFFE8E8E8);

        for (int lineIndex = 0;
                lineIndex < row.conditionLines.size();
                lineIndex++)
        {
            graphics.drawString(
                    this.font,
                    row.conditionLines.get(lineIndex),
                    layout.textX,
                    contentY + 12 + lineIndex * 9,
                    0xFFB8C8D8);
        }

        int buttonY = row.buttonY();
        drawButton(
                graphics,
                row,
                layout.buttonWidth,
                buttonY,
                row.containsButton(mouseX, mouseY));
    }

    private void drawButton(
            GuiGraphics graphics,
            Row row,
            int buttonWidth,
            int buttonY,
            boolean hovered)
    {
        int left = row.buttonX;
        int right = left + buttonWidth;
        int bottom = buttonY + BUTTON_HEIGHT;
        boolean enabled = row.buttonEnabled;

        graphics.fill(
                left - 1, buttonY - 1,
                right + 1, bottom + 1,
                enabled ? 0xFF101820 : 0xFF0A0D10);
        graphics.fill(
                left, buttonY,
                right, bottom,
                enabled ? 0xFFD9A441 : 0xFF3A4046);
        graphics.fill(
                left + 1, buttonY + 1,
                right - 1, bottom - 1,
                enabled
                        ? (hovered ? 0xFF3A556A : 0xFF223546)
                        : 0xFF25282C);

        if (enabled)
        {
            graphics.fill(
                    left + 2, buttonY + 2,
                    right - 2, buttonY + 3,
                    hovered ? 0xFF587387 : 0xFF4C657A);
            graphics.fill(
                    left + 2, bottom - 3,
                    right - 2, bottom - 2,
                    0xFF18242D);
            graphics.fill(
                    left + 2, buttonY + 2,
                    left + 3, bottom - 2,
                    0xFF8F6B2F);
        }

        int textColor = enabled
                ? (hovered ? 0xFFFFFFE6 : 0xFFFFE5A8)
                : 0xFF7A8288;
        graphics.drawCenteredString(
                this.font,
                row.buttonLabel,
                left + buttonWidth / 2,
                buttonY + (BUTTON_HEIGHT - 8) / 2,
                textColor);
    }

    private void drawScrollbar(GuiGraphics graphics, Layout layout)
    {
        int viewHeight = layout.contentBottom - layout.contentTop;
        int trackX = layout.panelX + layout.panelWidth - 3;
        graphics.fill(
                trackX, layout.contentTop,
                trackX + 2, layout.contentBottom,
                0x66000000);

        int thumbHeight = Math.max(
                12,
                viewHeight * viewHeight / layout.contentHeight);
        int available = viewHeight - thumbHeight;
        int thumbY = layout.contentTop
                + (available <= 0 ? 0
                : (int) ((double) available * scrollOffset
                / layout.scrollMax));
        graphics.fill(
                trackX, thumbY,
                trackX + 2, thumbY + thumbHeight,
                0xFFD8A84C);
    }

    private boolean isFullyVisible(Layout layout, Row row)
    {
        int screenY = row.screenY();
        return screenY >= layout.contentTop
                && screenY + row.height <= layout.contentBottom;
    }

    private Layout layout()
    {
        long resourceRevision = ReversalUiResources.revision();
        if (iconResourceRevision != resourceRevision)
        {
            Arrays.fill(icons, null);
            iconResourceRevision = resourceRevision;
        }
        int horizontalMargin = Math.max(
                4, Math.min(18, this.width / 36));
        int verticalMargin = Math.max(
                4, Math.min(18, this.height / 36));
        int availableWidth = Math.max(1, this.width - horizontalMargin * 2);
        int availableHeight = Math.max(1, this.height - verticalMargin * 2);
        int panelWidth = Math.min(MAX_PANEL_WIDTH, availableWidth);
        int panelHeight = Math.min(MAX_PANEL_HEIGHT, availableHeight);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        boolean hasIconSpace = panelWidth >= 260;
        int textX = panelX + (hasIconSpace ? 32 : 8);
        int buttonWidth = panelWidth < 260
                ? Math.max(50, panelWidth / 4)
                : Math.min(86, Math.max(66, panelWidth / 6));
        int buttonX = panelX + panelWidth - buttonWidth - 10;
        int textMaxWidth = Math.max(
                1, buttonX - textX - 8);
        int contentTop = panelY + CONTENT_TOP_OFFSET;
        int contentBottom = Math.max(
                contentTop,
                panelY + panelHeight - CONTENT_BOTTOM_OFFSET);
        int viewHeight = contentBottom - contentTop;

        List<Row> rows = new ArrayList<>(definitions.length);
        int rowTop = 0;
        int rowHeightHint = 36;
        for (int i = 0; i < definitions.length; i++)
        {
            Component condition = condition(i);
            List<FormattedCharSequence> lines = conditionLayouts.get(i).lines(
                    condition, textMaxWidth, this.font, Language.getInstance(), resourceRevision,
                    () -> this.font.split(condition, textMaxWidth));
            int conditionHeight = Math.max(8, lines.size() * 9);
            int height = Math.max(34, 16 + conditionHeight);
            rowHeightHint = Math.max(rowHeightHint, height);
            rows.add(new Row(
                    i,
                    rowTop,
                    height,
                    lines,
                    buttonLabel(i),
                    buttonEnabled(i),
                    buttonX,
                    buttonWidth,
                    contentTop,
                    buttonYWithin(height)));
            rowTop += height + 4;
        }

        int contentHeight = Math.max(1, rowTop - 4);
        int scrollMax = Math.max(
                0, contentHeight - viewHeight);
        scrollOffset = Math.max(
                0, Math.min(scrollMax, scrollOffset));

        return new Layout(
                panelX, panelY, panelWidth, panelHeight,
                contentTop, contentBottom,
                textX, buttonX, buttonWidth,
                hasIconSpace, rows,
                contentHeight, scrollMax, rowHeightHint);
    }

    private ItemStack icon(int index)
    {
        ItemStack icon = icons[index];
        if (icon == null || icon.getItem() != definitions[index].curseItem())
        {
            icon = BlessingNbt.createStack(definitions[index]);
            icons[index] = icon;
        }
        return icon;
    }

    private int buttonYWithin(int rowHeight)
    {
        return Math.max(0, (rowHeight - BUTTON_HEIGHT) / 2);
    }

    private final class Layout
    {
        private final int panelX;
        private final int panelY;
        private final int panelWidth;
        private final int panelHeight;
        private final int contentTop;
        private final int contentBottom;
        private final int textX;
        private final int buttonX;
        private final int buttonWidth;
        private final boolean hasIconSpace;
        private final List<Row> rows;
        private final int contentHeight;
        private final int scrollMax;
        private final int rowHeightHint;

        private Layout(
                int panelX, int panelY,
                int panelWidth, int panelHeight,
                int contentTop, int contentBottom,
                int textX, int buttonX, int buttonWidth,
                boolean hasIconSpace,
                List<Row> rows,
                int contentHeight, int scrollMax,
                int rowHeightHint)
        {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
            this.contentTop = contentTop;
            this.contentBottom = contentBottom;
            this.textX = textX;
            this.buttonX = buttonX;
            this.buttonWidth = buttonWidth;
            this.hasIconSpace = hasIconSpace;
            this.rows = rows;
            this.contentHeight = contentHeight;
            this.scrollMax = scrollMax;
            this.rowHeightHint = rowHeightHint;
        }
    }

    private final class Row
    {
        private final int index;
        private final int top;
        private final int height;
        private final List<FormattedCharSequence> conditionLines;
        private final Component buttonLabel;
        private final boolean buttonEnabled;
        private final int buttonX;
        private final int buttonWidth;
        private final int contentTop;
        private final int buttonYWithin;

        private Row(
                int index, int top, int height,
                List<FormattedCharSequence> conditionLines,
                Component buttonLabel, boolean buttonEnabled,
                int buttonX, int buttonWidth,
                int contentTop, int buttonYWithin)
        {
            this.index = index;
            this.top = top;
            this.height = height;
            this.conditionLines = conditionLines;
            this.buttonLabel = buttonLabel;
            this.buttonEnabled = buttonEnabled;
            this.buttonX = buttonX;
            this.buttonWidth = buttonWidth;
            this.contentTop = contentTop;
            this.buttonYWithin = buttonYWithin;
        }

        private int screenY()
        {
            return this.contentTop + top - scrollOffset;
        }

        private int buttonY()
        {
            return screenY() + buttonYWithin;
        }

        private boolean containsButton(double mouseX, double mouseY)
        {
            return mouseX >= buttonX
                    && mouseX <= buttonX + buttonWidth
                    && mouseY >= buttonY()
                    && mouseY <= buttonY() + BUTTON_HEIGHT;
        }
    }
}
