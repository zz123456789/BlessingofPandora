package com.blessingofpandora.client;

import com.blessingofpandora.LoveOrbRegistration;
import com.blessingofpandora.compat.love.LoveLedger;
import com.blessingofpandora.compat.love.LoveProgressEvents;
import com.blessingofpandora.network.LoveProgressS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Live, owner-private Love journal. Scrolling never rebuilds text or item data. */
@OnlyIn(Dist.CLIENT)
public class LoveHudScreen extends Screen
{
    private static final int INK = 0xFFE7DADD, MUTED = 0xFFB9A8AF, ROSE = 0xFFD5A0B2, WARNING = 0xFFE0A6A8;
    private static final int PAPER = 0xF51D1921, CARD = 0xFF29232C, EDGE = 0xFF604754, LINE = 11, PAD = 9;
    private final Screen parent;
    private final ItemStack inspected;
    private final Supplier<UUID> viewer;
    private final Supplier<LoveProgressS2CPacket> liveState;
    private LoveProgressS2CPacket cachedProgress;
    private UUID cachedViewer;
    private Language cachedLanguage;
    private long cachedResources = -1;
    private int cachedWidth = -1, cachedHeight = -1, revision, scroll;
    private boolean dirty = true, requested, evolved, conditions = true, personal, dragging, layered;
    private LoveHudLayout.Layout layout;
    private List<Card> cards = List.of();
    private List<Line> headerLines = List.of();
    private ItemStack icon = ItemStack.EMPTY;
    private LoveHudLayout.Rect powersTab, conditionsTab, statusBar;
    private FormattedCharSequence footer, powersLabel, conditionsLabel;

    private record Line(FormattedCharSequence text, int x, int y, int width, int color) {}
    private record Card(List<Line> lines, int height, boolean fullWidth, double progress, int accent) {}
    public record TextBounds(LoveHudLayout.Rect bounds, FormattedCharSequence text) {}
    public record CardSnapshot(LoveHudLayout.Rect bounds, List<TextBounds> text) {}
    public record LayoutSnapshot(LoveHudLayout.Rect panel, LoveHudLayout.Rect viewport, List<TextBounds> headerText,
                                 List<CardSnapshot> cards, int columns, int scrollMax, int scrollOffset,
                                 boolean evolved, boolean conditions, boolean personalData, int revision) {}

    public LoveHudScreen(Screen parent, ItemStack inspected)
    {
        this(parent, inspected, () -> Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID(),
                LoveProgressClient::current);
    }

    /** The injected display inputs also let client probes render the actual screen without a world. */
    public LoveHudScreen(Screen parent, ItemStack inspected, Supplier<UUID> viewer, Supplier<LoveProgressS2CPacket> liveState)
    {
        super(Component.translatable("blessingofpandora.love_hud.title"));
        this.parent = parent;
        this.inspected = inspected.copy();
        this.viewer = viewer;
        this.liveState = liveState;
    }

    protected LoveProgressS2CPacket progress() { return liveState.get(); }
    protected UUID viewerId() { return viewer.get(); }
    @Override protected void init()
    {
        dirty = true;
        if (!requested) { requested = true; LoveProgressClient.requestFresh(); }
    }
    @Override public boolean isPauseScreen() { return false; }
    public LoveHudScreen asGuiLayer() { layered = true; return this; }
    @Override public void onClose()
    {
        if (minecraft == null) return;
        if (parent instanceof AbstractContainerScreen<?> container && minecraft.player != null
                && container.getMenu() != minecraft.player.containerMenu)
        { minecraft.setScreen(null); return; }
        if (layered) ForgeHooksClient.popGuiLayer(minecraft);
        else minecraft.setScreen(parent);
    }
    @Override public void renderBackground(GuiGraphics graphics) {}
    public int layoutRevision() { ensureLayout(); return revision; }
    public int scrollOffset() { return scroll; }
    public boolean showingEvolved() { ensureLayout(); return evolved; }
    public boolean showingConditions() { ensureLayout(); return conditions; }

    private void ensureLayout()
    {
        LoveProgressS2CPacket data = progress();
        UUID viewerId = viewerId();
        Language language = Language.getInstance();
        long resources = ReversalUiResources.revision();
        if (!dirty && width == cachedWidth && height == cachedHeight && Objects.equals(data, cachedProgress)
                && Objects.equals(viewerId, cachedViewer) && language == cachedLanguage && resources == cachedResources) return;
        dirty = false;
        cachedProgress = data; cachedViewer = viewerId; cachedWidth = width; cachedHeight = height;
        cachedLanguage = language; cachedResources = resources;
        UUID owner = LoveProgressEvents.owner(inspected);
        boolean ownPacket = viewerId != null && data != null && viewerId.equals(data.owner());
        boolean foreign = owner != null && !owner.equals(viewerId);
        personal = ownPacket && owner != null && owner.equals(viewerId) && data.maid() != null;
        boolean nextEvolved = personal ? data.evolved() : LoveOrbRegistration.isEternal(inspected);
        if (layout == null || evolved != nextEvolved)
        {
            evolved = nextEvolved; conditions = !evolved; scroll = 0;
        }
        boolean knownWear = ownPacket && !foreign && (personal || owner == null && data.maid() == null);
        boolean banned = ownPacket && !foreign && data.banned();
        String status = foreign ? "foreign" : banned ? "banned" : !knownWear ? "waiting"
                : data.worn() ? "worn" : "unworn";
        icon = evolved ? LoveOrbRegistration.createEternalStack() : LoveOrbRegistration.createStack();
        if (inspected.hasTag() && !icon.isEmpty()) icon.setTag(inspected.getTag().copy());
        if (icon.isEmpty()) icon = inspected.copy();

        int panelWidth = LoveHudLayout.panelWidth(width);
        int titleWidth = Math.max(1, panelWidth - 76);
        List<FormattedCharSequence> name = font.split(icon.getHoverName(), titleWidth);
        int nameHeight = Math.max(22, name.size() * LINE);
        List<FormattedCharSequence> statusLines = font.split(tr("status." + status), Math.max(1, panelWidth - 44));
        int statusHeight = 10 + statusLines.size() * LINE;
        int headerHeight = nameHeight + 7 + statusHeight + (evolved ? 25 : 0);
        var geometry = LoveHudLayout.geometry(width, height, headerHeight);
        var header = geometry.header();
        ArrayList<Line> headings = new ArrayList<>();
        int nameY = header.y() + Math.max(0, (22 - name.size() * LINE) / 2);
        for (var line : name) { headings.add(new Line(line, header.x() + 30, nameY, font.width(line), INK)); nameY += LINE; }
        statusBar = new LoveHudLayout.Rect(header.x(), header.y() + nameHeight + 7, header.width(), statusHeight);
        int statusY = statusBar.y() + 5;
        for (var line : statusLines)
        { headings.add(new Line(line, statusBar.x() + 7, statusY, font.width(line), banned ? WARNING : MUTED)); statusY += LINE; }
        headerLines = List.copyOf(headings);
        int tabWidth = (header.width() - 6) / 2, tabY = statusBar.bottom() + 5;
        powersTab = new LoveHudLayout.Rect(header.x(), tabY, tabWidth, 18);
        conditionsTab = new LoveHudLayout.Rect(header.x() + tabWidth + 6, tabY, header.width() - tabWidth - 6, 18);
        powersLabel = language.getVisualOrder(font.substrByWidth(tr("tab.powers"), Math.max(1, tabWidth - 8)));
        conditionsLabel = language.getVisualOrder(font.substrByWidth(tr("tab.conditions"), Math.max(1, tabWidth - 8)));
        footer = language.getVisualOrder(font.substrByWidth(tr("footer"), geometry.footer().width()));

        ArrayList<Card> prepared = new ArrayList<>();
        String intro = (evolved ? tr("intro.eternal") : tr("intro.love")).getString();
        if (!personal) intro += "\n" + tr(foreign ? "private" : "unbound").getString();
        prepared.add(card(tr("overview").getString(), intro, "", Double.NaN, true, ROSE, geometry));
        if (conditions) addConditions(prepared, personal ? data : null, geometry);
        else for (int i = 1; i <= 7; i++)
        {
            String full = Component.translatable("item.blessingofpandora.eternal_love.effect." + i).getString();
            int split = full.indexOf('：'); if (split < 0) split = full.indexOf(':');
            prepared.add(card(split < 0 ? full : full.substring(0, split), split < 0 ? "" : full.substring(split + 1).trim(),
                    "", Double.NaN, i == 7, ROSE, geometry));
        }
        prepared.add(card(tr("obtain.title").getString(), tr("obtain.body").getString(), "", Double.NaN, true, MUTED, geometry));
        String binding = personal ? tr("binding.own", data.maidName(), data.maid().toString()).getString()
                : tr(foreign ? "binding.foreign" : "binding.none").getString();
        prepared.add(card(tr("binding.title").getString(), binding, "", Double.NaN, true, ROSE, geometry));
        String safety = tr("safety.body").getString();
        if (evolved) safety += "\n" + Component.translatable("item.blessingofpandora.eternal_love.revoke").getString()
                + "\n" + tr("eternal_limits").getString();
        prepared.add(card(tr("safety.title").getString(), safety, "", Double.NaN, true, MUTED, geometry));
        prepared.add(card(tr("penalty.title").getString(), tr("penalty.body").getString(), "", Double.NaN, true, WARNING, geometry));
        cards = List.copyOf(prepared);
        layout = LoveHudLayout.arrange(geometry, cards.stream().map(c -> new LoveHudLayout.Size(c.height(), c.fullWidth())).toList());
        scroll = Mth.clamp(scroll, 0, layout.scrollMax());
        revision++;
    }

    private void addConditions(List<Card> output, LoveProgressS2CPacket data, LoveHudLayout.Geometry geometry)
    {
        for (int i = 1; i <= 7; i++)
        {
            String status = tr("progress.private").getString();
            double ratio = Double.NaN;
            int accent = ROSE;
            if (data != null)
            {
                switch (i)
                {
                    case 1 -> { status = time(data.time()) + " / 24:00:00"; ratio = (double)data.time() / LoveLedger.ETERNITY_TICKS; }
                    case 2 -> { status = tr(data.dead() ? "progress.dead" : "progress.alive").getString(); ratio = data.dead() ? 0 : 1; if (data.dead()) accent = WARNING; }
                    case 3 -> { status = String.format(Locale.ROOT, "%,.1f / 1,314,520", data.damage()); ratio = data.damage() / LoveLedger.DAMAGE_TARGET; }
                    case 4 -> { status = tr("progress.games", mark(data.games(), 1), mark(data.games(), 2), mark(data.games(), 4)).getString(); ratio = Integer.bitCount(data.games() & 7) / 3D; }
                    case 5 -> { status = time(data.romance()) + " / >00:30:00"; ratio = (double)data.romance() / (LoveLedger.ROMANCE_TICKS + 1); }
                    case 6 -> status = tr(data.started() ? "progress.jealousy_active" : "progress.not_started").getString();
                    case 7 -> { status = tr(data.oath() ? "progress.oath" : "progress.oath_wait").getString(); ratio = data.oath() ? 1 : 0; }
                }
            }
            output.add(card(tr("condition." + i + ".title").getString(), tr("condition." + i + ".body").getString(), status,
                    ratio, i == 7, accent, geometry));
        }
    }

    private Card card(String title, String body, String status, double ratio, boolean fullWidth, int accent, LoveHudLayout.Geometry geometry)
    {
        int width = (fullWidth ? geometry.viewport().width() : geometry.columnWidth()) - PAD * 2;
        ArrayList<Line> lines = new ArrayList<>();
        int y = PAD;
        y = addLines(lines, title, width, y, accent) + 5;
        y = addLines(lines, body.replace("\n   ", "\n"), width, y, INK);
        if (!status.isEmpty()) y = addLines(lines, status, width, y + 7, MUTED);
        if (!Double.isNaN(ratio)) y += 10;
        return new Card(List.copyOf(lines), y + PAD, fullWidth, ratio, accent);
    }
    private int addLines(List<Line> lines, String text, int width, int y, int color)
    {
        if (text.isEmpty()) return y;
        for (String paragraph : text.split("\n", -1))
        {
            if (paragraph.isEmpty()) { y += 5; continue; }
            for (var sequence : font.split(Component.literal(paragraph), Math.max(1, width)))
            { lines.add(new Line(sequence, PAD, y, font.width(sequence), color)); y += LINE; }
        }
        return y;
    }
    private static Component tr(String key, Object... arguments) { return Component.translatable("blessingofpandora.love_hud." + key, arguments); }
    private static Component mark(int games, int bit) { return tr((games & bit) != 0 ? "progress.won" : "progress.not_won"); }
    private static String time(long ticks)
    {
        long seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ensureLayout();
        var geometry = layout.geometry();
        graphics.fill(0, 0, width, height, 0x8A0C0A10);
        frame(graphics, geometry.panel(), PAPER, EDGE);
        var h = geometry.header();
        frame(graphics, new LoveHudLayout.Rect(h.x(), h.y(), 23, 23), CARD, EDGE);
        graphics.renderItem(icon, h.x() + 3, h.y() + 3);
        frame(graphics, statusBar, 0xFF302730, 0xFF493641);
        for (Line line : headerLines) graphics.drawString(font, line.text(), line.x(), line.y(), line.color(), false);
        var close = geometry.close();
        frame(graphics, close, close.contains(mouseX, mouseY) ? 0xFF60424F : CARD, EDGE);
        graphics.drawCenteredString(font, "×", close.x() + close.width() / 2, close.y() + 3, MUTED);
        if (evolved) { drawTab(graphics, powersTab, powersLabel, !conditions, mouseX, mouseY); drawTab(graphics, conditionsTab, conditionsLabel, conditions, mouseX, mouseY); }
        var viewport = geometry.viewport();
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try
        {
            for (int i = 0; i < cards.size(); i++)
            {
                Card card = cards.get(i);
                var bounds = layout.cards().get(i).offsetY(-scroll);
                if (bounds.bottom() <= viewport.y() || bounds.y() >= viewport.bottom()) continue;
                frame(graphics, bounds, CARD, 0xFF44343F);
                graphics.fill(bounds.x(), bounds.y() + 1, bounds.x() + 2, bounds.bottom() - 1, card.accent());
                for (Line line : card.lines()) graphics.drawString(font, line.text(), bounds.x() + line.x(), bounds.y() + line.y(), line.color(), false);
                if (!Double.isNaN(card.progress()))
                {
                    int left = bounds.x() + PAD, right = bounds.right() - PAD, top = bounds.bottom() - PAD - 5;
                    graphics.fill(left, top, right, top + 4, 0xFF17141B);
                    graphics.fill(left, top, left + (int)Math.round((right - left) * Mth.clamp(card.progress(), 0, 1)), top + 4, card.accent());
                }
            }
        }
        finally { graphics.disableScissor(); }
        if (layout.scrollMax() > 0)
        {
            var track = scrollTrack();
            graphics.fill(track.x(), track.y(), track.right(), track.bottom(), 0xFF392B35);
            int thumb = Math.max(14, (int)((long)track.height() * viewport.height() / Math.max(1, layout.contentHeight())));
            int top = track.y() + (int)((long)Math.max(0, track.height() - thumb) * scroll / layout.scrollMax());
            graphics.fill(track.x(), top, track.right(), top + thumb, ROSE);
        }
        graphics.drawString(font, footer, geometry.footer().x(), geometry.footer().y(), MUTED, false);
    }

    private static void frame(GuiGraphics graphics, LoveHudLayout.Rect rect, int fill, int border)
    {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), border);
        graphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, fill);
    }
    private void drawTab(GuiGraphics graphics, LoveHudLayout.Rect rect, FormattedCharSequence text, boolean active, int mouseX, int mouseY)
    {
        frame(graphics, rect, active ? 0xFF503844 : rect.contains(mouseX, mouseY) ? 0xFF3E2E39 : CARD, active ? ROSE : EDGE);
        graphics.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + 5, active ? INK : MUTED, false);
    }
    private LoveHudLayout.Rect scrollTrack()
    {
        var viewport = layout.geometry().viewport();
        return new LoveHudLayout.Rect(viewport.right() + 3, viewport.y(), 3, viewport.height());
    }
    private void selectConditions(boolean selected)
    {
        if (conditions != selected) { conditions = selected; dirty = true; scroll = 0; }
    }
    @Override public boolean mouseClicked(double x, double y, int button)
    {
        ensureLayout();
        if (button == 0)
        {
            if (layout.geometry().close().contains(x, y)) { onClose(); return true; }
            if (evolved && powersTab.contains(x, y)) { selectConditions(false); return true; }
            if (evolved && conditionsTab.contains(x, y)) { selectConditions(true); return true; }
            var track = scrollTrack();
            if (layout.scrollMax() > 0 && x >= track.x() - 2 && x < track.right() + 2 && y >= track.y() && y < track.bottom())
            { dragging = true; dragScroll(y); return true; }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta)
    {
        ensureLayout();
        if (layout.scrollMax() <= 0 || delta == 0) return false;
        scroll = Mth.clamp(scroll - (int)Math.round(delta * 28), 0, layout.scrollMax());
        return true;
    }
    private void dragScroll(double mouseY)
    {
        var track = scrollTrack();
        scroll = Mth.clamp((int)Math.round((mouseY - track.y()) / Math.max(1, track.height()) * layout.scrollMax()), 0, layout.scrollMax());
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy)
    { if (dragging && button == 0) { dragScroll(y); return true; } return super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseReleased(double x, double y, int button)
    { if (button == 0 && dragging) { dragging = false; return true; } return super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers)
    {
        ensureLayout();
        if (evolved && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) { selectConditions(key == GLFW.GLFW_KEY_RIGHT); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP)
        { scroll = Mth.clamp(scroll + (key == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1) * Math.max(20, layout.geometry().viewport().height() - 20), 0, layout.scrollMax()); return true; }
        if (key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END) { scroll = key == GLFW.GLFW_KEY_HOME ? 0 : layout.scrollMax(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }

    /** Actual Font.split line bounds, in screen coordinates after scrolling, for visual verification. */
    public LayoutSnapshot layoutSnapshot()
    {
        ensureLayout();
        ArrayList<CardSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++)
        {
            var bounds = layout.cards().get(i).offsetY(-scroll);
            List<TextBounds> text = cards.get(i).lines().stream().map(line -> new TextBounds(
                    new LoveHudLayout.Rect(bounds.x() + line.x(), bounds.y() + line.y(), line.width(), LINE), line.text())).toList();
            snapshots.add(new CardSnapshot(bounds, text));
        }
        List<TextBounds> header = headerLines.stream().map(line -> new TextBounds(
                new LoveHudLayout.Rect(line.x(), line.y(), line.width(), LINE), line.text())).toList();
        var geometry = layout.geometry();
        return new LayoutSnapshot(geometry.panel(), geometry.viewport(), header, List.copyOf(snapshots), geometry.columns(),
                layout.scrollMax(), scroll, evolved, conditions, personal, revision);
    }
}
