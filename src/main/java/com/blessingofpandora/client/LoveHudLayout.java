package com.blessingofpandora.client;

import java.util.ArrayList;
import java.util.List;

/** Pixel geometry only. Text is measured once by the screen before these rows are arranged. */
public final class LoveHudLayout
{
    public static final int GAP = 8;
    private LoveHudLayout() {}

    public record Rect(int x, int y, int width, int height)
    {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
        public Rect offsetY(int offset) { return new Rect(x, y + offset, width, height); }
    }
    public record Size(int height, boolean fullWidth) {}
    public record Geometry(Rect panel, Rect header, Rect viewport, Rect footer, Rect close, int columns, int columnWidth) {}
    public record Layout(Geometry geometry, List<Rect> cards, int contentHeight, int scrollMax) {}

    public static int panelWidth(int screenWidth)
    {
        int margin = screenWidth < 360 ? 6 : 12;
        return Math.max(1, Math.min(620, screenWidth - margin * 2));
    }

    public static Geometry geometry(int screenWidth, int screenHeight, int requestedHeaderHeight)
    {
        int width = panelWidth(screenWidth);
        int height = Math.max(1, Math.min(470, screenHeight - (screenHeight < 240 ? 12 : 24)));
        int x = (screenWidth - width) / 2, y = (screenHeight - height) / 2;
        Rect panel = new Rect(x, y, width, height);
        int inset = Math.min(10, Math.max(2, width / 8));
        int headerHeight = Math.min(requestedHeaderHeight, Math.max(24, height - 52));
        Rect header = new Rect(x + inset, y + 10, Math.max(1, width - inset * 2), headerHeight);
        Rect footer = new Rect(x + inset, y + height - 18, Math.max(1, width - inset * 2), 10);
        int top = header.bottom() + 8;
        Rect viewport = new Rect(x + inset, top, Math.max(1, width - inset * 2 - 6), Math.max(1, footer.y() - top - 8));
        int columns = viewport.width() >= 440 ? 2 : 1;
        int columnWidth = (viewport.width() - (columns - 1) * GAP) / columns;
        return new Geometry(panel, header, viewport, footer,
                new Rect(panel.right() - inset - 14, y + 10, 14, 14), columns, columnWidth);
    }

    public static Layout arrange(Geometry geometry, List<Size> sizes)
    {
        List<Rect> cards = new ArrayList<>(sizes.size());
        Rect viewport = geometry.viewport();
        int y = viewport.y();
        for (int i = 0; i < sizes.size(); )
        {
            Size first = sizes.get(i);
            if (geometry.columns() == 2 && !first.fullWidth() && i + 1 < sizes.size() && !sizes.get(i + 1).fullWidth())
            {
                int height = Math.max(first.height(), sizes.get(i + 1).height());
                cards.add(new Rect(viewport.x(), y, geometry.columnWidth(), height));
                cards.add(new Rect(viewport.x() + geometry.columnWidth() + GAP, y, geometry.columnWidth(), height));
                y += height + GAP;
                i += 2;
            }
            else
            {
                int width = first.fullWidth() || geometry.columns() == 1 ? viewport.width() : geometry.columnWidth();
                cards.add(new Rect(viewport.x(), y, width, first.height()));
                y += first.height() + GAP;
                i++;
            }
        }
        int contentHeight = Math.max(0, y - viewport.y() - (sizes.isEmpty() ? 0 : GAP));
        return new Layout(geometry, List.copyOf(cards), contentHeight, Math.max(0, contentHeight - viewport.height()));
    }
}
