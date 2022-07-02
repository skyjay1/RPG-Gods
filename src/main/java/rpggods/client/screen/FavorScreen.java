package rpggods.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.deity.Deity;
import rpggods.deity.DeityHelper;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.entity.AltarEntity;
import rpggods.favor.FavorLevel;
import rpggods.favor.FavorRange;
import rpggods.favor.IFavor;
import rpggods.gui.FavorContainer;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class FavorScreen extends ContainerScreen<FavorContainer> {

    // CONSTANTS
    private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/gui/favor/favor.png");
    private static final ResourceLocation SCREEN_WIDGETS = new ResourceLocation(RPGGods.MODID, "textures/gui/favor/favor_widgets.png");

    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 168;

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_COUNT = 6;

    private static final int PAGE_WIDTH = 42;
    private static final int PAGE_HEIGHT = 32;
    private static final int PAGE_COUNT = 4;

    private static final int ARROW_WIDTH = 12;
    private static final int ARROW_HEIGHT = 17;

    private static final int SCROLL_X = 216;
    private static final int SCROLL_Y = 30;
    private static final int SCROLL_WIDTH = 14;
    private static final int SCROLL_HEIGHT = 120;


    // Header
    private static final int NAME_X = 33;
    private static final int NAME_Y = 15;
    private static final int FAVOR_X = 184;
    private static final int FAVOR_Y = NAME_Y;

    // Summary page
    private static final int TITLE_X = NAME_X;
    private static final int TITLE_Y = 30;
    private static final int PREVIEW_X = NAME_X;
    private static final int PREVIEW_Y = 44;
    private static final int PREVIEW_WIDTH = 54;
    private static final int PREVIEW_HEIGHT = 80;
    private static final int SUMMARY_X = PREVIEW_X + PREVIEW_WIDTH + 32;
    private static final int SUMMARY_Y = PREVIEW_Y;

    // Offerings page
    private static final int OFFERING_X = 32;
    private static final int OFFERING_Y = 50;
    private static final int OFFERING_WIDTH = 18 * 3;
    private static final int OFFERING_HEIGHT = 18;
    private static final int TRADE_X = 140;
    private static final int TRADE_Y = OFFERING_Y;
    private static final int TRADE_WIDTH = 18 * 4;
    private static final int OFFERING_COUNT = 12;
    private static final int TRADE_COUNT = 6;

    // Sacrifices page
    private static final int SACRIFICE_X = 32;
    private static final int SACRIFICE_Y = 50;
    private static final int SACRIFICE_WIDTH = 18 * 9;
    private static final int SACRIFICE_HEIGHT = 16;
    private static final int SACRIFICE_COUNT = 7;

    // Perks page
    private static final int PERK_WIDTH = 22;
    private static final int PERK_HEIGHT = 22;
    private static final int PERK_SPACE_X = 6;
    private static final int PERK_SPACE_Y = 1;
    private static final int PERK_TOOLTIP_WIDTH = 101;
    private static final int PERK_TOOLTIP_HEIGHT = 122;
    private static final int PERK_BOUNDS_X = 23;
    private static final int PERK_BOUNDS_Y = 41;
    private static final int PERK_BOUNDS_WIDTH = 209;
    private static final int PERK_BOUNDS_HEIGHT = SCREEN_HEIGHT - PERK_BOUNDS_Y - PERK_HEIGHT;

    // Data
    private static final List<ResourceLocation> deityList = new ArrayList<>();
    private static final Map<ResourceLocation, AltarEntity> entityMap = new HashMap<>();
    private static final Map<ResourceLocation, List<ImmutablePair<ResourceLocation, Offering>>> offeringMap = new HashMap();
    private static final Map<ResourceLocation, List<ImmutablePair<ResourceLocation, Offering>>> tradeMap = new HashMap();
    private static final Map<ResourceLocation, List<ImmutablePair<ResourceLocation, Sacrifice>>> sacrificeMap = new HashMap();
    // Key: deity ID; Value: Map of perk level to list of available perks
    private static final Map<ResourceLocation, Map<Integer, List<Perk>>> perkMap = new HashMap();
    private ResourceLocation deity;
    private IFormattableTextComponent deityName = (IFormattableTextComponent) StringTextComponent.EMPTY;
    private IFormattableTextComponent deityFavor = (IFormattableTextComponent) StringTextComponent.EMPTY;
    private long openTimestamp;
    private int tabGroupCount;
    private int tabGroup;
    private int tabCount;
    private int tab;
    private Page page = Page.SUMMARY;

    // UI elements
    private final FavorScreen.TabButton[] tabButtons = new FavorScreen.TabButton[TAB_COUNT];
    private final FavorScreen.PageButton[] pageButtons = new FavorScreen.PageButton[PAGE_COUNT];
    private FavorScreen.TabArrowButton leftButton;
    private FavorScreen.TabArrowButton rightButton;
    private ScrollButton scrollButton;
    private boolean scrollVisible;
    private boolean scrollEnabled;

    // Summary page
    private IFormattableTextComponent deityTitle = (IFormattableTextComponent) StringTextComponent.EMPTY;

    // Offering page
    private TextButton offeringTitle;
    private TextButton tradeTitle;
    private final FavorScreen.OfferingButton[] offeringButtons = new FavorScreen.OfferingButton[OFFERING_COUNT];
    private final FavorScreen.TradeButton[] tradeButtons = new FavorScreen.TradeButton[TRADE_COUNT];
    private int offeringCount;
    private int tradeCount;

    // Sacrifice page
    private TextButton sacrificeTitle;
    private final FavorScreen.SacrificeButton[] sacrificeButtons = new FavorScreen.SacrificeButton[SACRIFICE_COUNT];
    private int sacrificeCount;

    // Perk page
    private final Map<ResourceLocation, List<FavorScreen.PerkButton>> perkButtonMap = new HashMap<>();
    private final Map<Integer, FavorScreen.TextButton> perkLevelButtonMap = new HashMap<>();
    private boolean isDraggingPerks;
    private int dx;
    private int dy;


    public FavorScreen(FavorContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        // add all deities to list
        final IFavor favor = screenContainer.getFavor();

        // clear maps to prepare for updated values
        deityList.clear();
        offeringMap.clear();
        tradeMap.clear();
        sacrificeMap.clear();
        perkMap.clear();

        // Iterate over all deities using their deity helper.
        // This allows us to skip items that were invalidated by the deity helper, such as empty offerings or perks.
        for(DeityHelper deityHelper : RPGGods.DEITY_HELPER.values()) {
            // skip deities that are not enabled or not unlocked
            Deity d = deityHelper.getDeity().orElse(Deity.EMPTY);
            if(!d.isEnabled() || !favor.getFavor(deityHelper.id).isEnabled()) {
                continue;
            }
            // add deity to list
            deityList.add(deityHelper.id);
            // add all offerings to map using deity helper (so we can skip offerings that were invalid)
            for(List<ResourceLocation> entry : deityHelper.offeringMap.values()) {
                for(ResourceLocation offeringId : entry) {
                    Optional<Offering> optional = RPGGods.OFFERING.get(offeringId);
                    optional.ifPresent(offering -> {
                        // determine which map to use (offering or trade)
                        Map<ResourceLocation, List<ImmutablePair<ResourceLocation, Offering>>> map = offering.getTrade().isPresent() ? tradeMap : offeringMap;
                        // add the offering to the map
                        map.computeIfAbsent(Offering.getDeity(deityHelper.id), id -> Lists.newArrayList()).add(ImmutablePair.of(deityHelper.id, offering));
                    });
                }
            }
            // add all sacrifices to map using deity helper (so we can skip sacrifices that were invalid)
            for(List<ResourceLocation> entry : deityHelper.sacrificeMap.values()) {
                for(ResourceLocation sacrificeId : entry) {
                    Optional<Sacrifice> optional = RPGGods.SACRIFICE.get(sacrificeId);
                    optional.ifPresent(sacrifice -> {
                        // add the sacrifice to the map
                        sacrificeMap.computeIfAbsent(Sacrifice.getDeity(deityHelper.id), id -> Lists.newArrayList()).add(ImmutablePair.of(deityHelper.id, sacrifice));
                    });
                }
            }
            // add all non-hidden perks to map using deity helper (so we can skip perks that were invalid)
            Perk perk;
            for(ResourceLocation entry : deityHelper.perkList) {
                Optional<Perk> optional = RPGGods.PERK.get(entry);
                if(optional.isPresent()) {
                    perk = optional.get();
                    // skip hidden perks
                    if(perk.getIcon().isHidden()) {
                        continue;
                    }
                    // add entry to perk map if absent
                    if(!perkMap.containsKey(deityHelper.id)) {
                        // add map with keys for all levels
                        FavorLevel favorLevel = favor.getFavor(perk.getDeity());
                        Map<Integer, List<Perk>> map = new HashMap<>();
                        for(int i = favorLevel.getMin(), j = favorLevel.getMax(); i <= j; i++) {
                            map.put(i, Lists.newArrayList());
                        }
                        perkMap.put(deityHelper.id, map);
                    }
                    // determine which level to place the perk
                    int min = perk.getRange().getMinLevel();
                    int max = perk.getRange().getMaxLevel();
                    int unlock;
                    if(min >= 0 && max >= 0) unlock = min;
                    else if(min <= 0 && max <= 0) unlock = max;
                    else unlock = Math.min(Math.abs(min), Math.abs(max));
                    // actually add the perk to the map
                    if(perkMap.get(deityHelper.id).containsKey(unlock)) {
                        perkMap.get(deityHelper.id).get(unlock).add(perk);
                    }
                }
            }
        }
        // sort deity list
        deityList.sort((d1, d2) -> favor.getFavor(d2).compareToAbs(favor.getFavor(d1)));
        // update offering and trade counts
        offeringCount = OFFERING_COUNT;
        tradeCount = TRADE_COUNT;
        // sort offerings by favor (descending)
        offeringMap.values().forEach(l -> Collections.sort(l, (t1, t2) -> t2.getRight().getFavor() - t1.getRight().getFavor()));
        // sort trades by unlock level (ascending)
        tradeMap.values().forEach(l -> Collections.sort(l, (t1, t2) -> t1.getRight().getTradeMinLevel() - t2.getRight().getTradeMinLevel()));
        // update sacrifice counts
        sacrificeCount = SACRIFICE_COUNT;
        // sort sacrifices by favor (descending)
        sacrificeMap.values().forEach(l -> Collections.sort(l, (t1, t2) -> t2.getRight().getFavor() - t1.getRight().getFavor()));

        // determine current page
        if(deityList.size() > 0) {
            deity = screenContainer.getDeity().orElse(deityList.get(0));
            // ensure deity is unlocked
            Deity d = RPGGods.DEITY.get(deity).orElse(Deity.EMPTY);
            if(!d.isUnlocked() || !d.isEnabled()) {
                deity = deityList.get(0);
            }
        }

        // initialize number of tabs
        tabCount = Math.min(deityList.size(), TAB_COUNT);
        tabGroupCount = Math.max(1, (int)Math.ceil((double)deityList.size() / (double)TAB_COUNT));
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        this.inventoryLabelY = this.height;
        this.openTimestamp = inventory.player.level.getGameTime();
        // clear button lists and maps
        perkButtonMap.clear();
        perkLevelButtonMap.clear();
        // add tabs
        int startX = (this.imageWidth - (TAB_WIDTH * TAB_COUNT)) / 2;
        int startY;
        for(int i = 0; i < tabCount; i++) {
            tabButtons[i] = this.addButton(new TabButton(this, i, new TranslationTextComponent(Altar.createTranslationKey(deityList.get(i))),
                    leftPos + startX + (i * TAB_WIDTH), topPos - TAB_HEIGHT + 13));
        }
        // add tab buttons
        leftButton = this.addButton(new TabArrowButton(this, leftPos + startX - (ARROW_WIDTH + 4), topPos - TAB_HEIGHT + 20, true));
        rightButton = this.addButton(new TabArrowButton(this, leftPos + startX + TAB_WIDTH * TAB_COUNT + 4, topPos - TAB_HEIGHT + 20, false));
        // add pages
        startX = (this.imageWidth - (PAGE_WIDTH * PAGE_COUNT)) / 2;
        for(int i = 0; i < PAGE_COUNT; i++) {
            FavorScreen.Page p = FavorScreen.Page.values()[i];
            pageButtons[i] = this.addButton(new PageButton(this, leftPos + startX + (i * PAGE_WIDTH), topPos + SCREEN_HEIGHT - 7,
                    125 + 18 * i, 130, new TranslationTextComponent(p.getTitle()), new TranslationTextComponent(p.getTitle() + ".tooltip"), p));
        }
        // add scroll bar
        scrollButton = this.addButton(new ScrollButton<>(this, this.leftPos + SCROLL_X, this.topPos + SCROLL_Y, SCROLL_WIDTH, SCROLL_HEIGHT,
                0, 165, SCREEN_WIDGETS, true, gui -> gui.scrollEnabled, b -> updateScroll(b.getScrollAmount())));
        // re-usable text components
        ITextComponent text;
        ITextComponent tooltip;
        // add offering UI elements
        text = new TranslationTextComponent("gui.favor.offerings").withStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.offerings.tooltip");
        offeringTitle = this.addButton(new TextButton(this, this.leftPos + OFFERING_X, this.topPos + OFFERING_Y - 16, OFFERING_WIDTH * 2, 12, text, tooltip));
        text = new TranslationTextComponent("gui.favor.trades").withStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.trades.tooltip");
        tradeTitle = this.addButton(new TextButton(this, this.leftPos + TRADE_X, this.topPos + TRADE_Y - 16, TRADE_WIDTH, 12, text, tooltip));
        for(int i = 0; i < OFFERING_COUNT; i++) {
            offeringButtons[i] = this.addButton(new OfferingButton(this, i, this.leftPos + OFFERING_X + OFFERING_WIDTH * (i % 2), this.topPos + OFFERING_Y + OFFERING_HEIGHT * (i / 2)));
        }
        for(int i = 0; i < TRADE_COUNT; i++) {
            tradeButtons[i] = this.addButton(new TradeButton(this, i, this.leftPos + TRADE_X, this.topPos + TRADE_Y + i * OFFERING_HEIGHT));
        }
        // add sacrifice UI elements
        text = new TranslationTextComponent("gui.favor.sacrifices").withStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.sacrifices.tooltip");
        sacrificeTitle = this.addButton(new TextButton(this, this.leftPos + SACRIFICE_X, this.topPos + SACRIFICE_Y - 16, SACRIFICE_WIDTH, 12, text, tooltip));
        for(int i = 0; i < SACRIFICE_COUNT; i++) {
            sacrificeButtons[i] = this.addButton(new SacrificeButton(this, i, this.leftPos + SACRIFICE_X, this.topPos + SACRIFICE_Y + i * SACRIFICE_HEIGHT));
        }
        // add perk UI elements
        for(Map.Entry<ResourceLocation, Map<Integer, List<Perk>>> entry : perkMap.entrySet()) {
            startX = this.leftPos + PERK_BOUNDS_X;
            startY = this.topPos + PERK_BOUNDS_Y;
            // each entry is (favorLevel, perksAtLevel)
            for(Map.Entry<Integer, List<Perk>> perksAtLevel : entry.getValue().entrySet()) {
                // determine which button list to use
                List<PerkButton> perkButtonList = perkButtonMap.computeIfAbsent(entry.getKey(), id -> Lists.newArrayList());
                // determine how many buttons are already in this list
                int perkCount = 0;
                // add each perk to the list using perkCount to determine y-position
                for(Perk p : perksAtLevel.getValue()) {
                    perkButtonList.add(this.addButton(new PerkButton(this, p,
                            startX + perksAtLevel.getKey() * (PERK_WIDTH + PERK_SPACE_X),
                            startY + PERK_SPACE_Y + perkCount * (PERK_HEIGHT + PERK_SPACE_Y))));
                    perkCount++;
                }
                // add level number button
                text = new StringTextComponent(perksAtLevel.getKey().toString()).withStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
                if(!perkLevelButtonMap.containsKey(perksAtLevel.getKey())) {
                    perkLevelButtonMap.put(perksAtLevel.getKey(), this.addButton(
                            new TextButton(this, startX + perksAtLevel.getKey() * (PERK_WIDTH + PERK_SPACE_X) + 4, startY - 12,
                                    PERK_WIDTH, PERK_HEIGHT, text)));
                }
            }
        }
        // determine current tab
        int index = Math.max(0, deityList.indexOf(deity));
        updateTabGroup(index / tabCount);
        updateTab(index % tabCount);
        updatePage(Page.SUMMARY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y) { }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        // draw background image
        this.getMinecraft().getTextureManager().bind(SCREEN_TEXTURE);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        // draw name
        this.font.drawShadow(matrixStack, deityName, this.leftPos + NAME_X, this.topPos + NAME_Y, 0xFFFFFF);
        // draw favor
        this.font.draw(matrixStack, deityFavor, this.leftPos + FAVOR_X, this.topPos + FAVOR_Y, 0xFFFFFF);
        // render page-specific items
        switch (this.page) {
            case SUMMARY:
                renderSummaryPage(matrixStack, mouseX, mouseY, partialTicks);
                break;
            case OFFERINGS:
                renderOfferingsPage(matrixStack);
                break;
            case SACRIFICES:
                renderSacrificesPage(matrixStack);
                break;
            case PERKS:
                // Perks page is rendered later
                break;
        }
        // draw widgets
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw perk page UI elements AFTER widgets and on top of everything
        if(this.page == Page.PERKS) {
            renderPerksPage(matrixStack, mouseX, mouseY, partialTicks);
        }
        // draw hovering text LAST
        for(Widget b : this.buttons) {
            if(b.visible && b.isHovered()) {
                b.renderToolTip(matrixStack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        double multiplier = 1.0D;
        if(page == Page.OFFERINGS) {
            float maxSize = (float) Math.max(offeringCount / 2, tradeCount);
            multiplier = 1.0F / maxSize;
        } else if(page == Page.SACRIFICES) {
            multiplier = 1.0F / (float) sacrificeCount;
        }
        return this.scrollButton.mouseScrolled(mouseX, mouseY, scrollAmount * multiplier);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.page == Page.PERKS && mouseX > leftPos + PERK_BOUNDS_X && mouseX < leftPos + PERK_BOUNDS_X + PERK_BOUNDS_WIDTH
                && mouseY > topPos + PERK_BOUNDS_Y && mouseY < topPos + SCREEN_HEIGHT) {
            isDraggingPerks = true;
        }
        return !isDraggingPerks && super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = isDraggingPerks;
        isDraggingPerks = false;
        return !wasDragging && super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // if mouse is within bounds, attempt to move all perks currently on the screen
        if(isDraggingPerks) {
            int moveX = (int)Math.round(dragX);
            int moveY = (int)Math.round(dragY);
            updateMove(moveX, moveY);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void removed() {
        super.removed();
        // clear entity map
        for(AltarEntity entity : entityMap.values()) {
            entity.remove();
        }
    }

    private void renderSummaryPage(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // draw title
        int startX = (this.width - this.font.width(deityTitle)) / 2;
        this.font.draw(matrixStack, deityTitle, startX, this.topPos + TITLE_Y, 0xFFFFFF);
        // draw preview pane
        this.minecraft.getTextureManager().bind(SCREEN_WIDGETS);
        this.blit(matrixStack, this.leftPos + PREVIEW_X, this.topPos + PREVIEW_Y, 202, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        // prepare to draw favor amounts
        final FavorLevel level = getMenu().getFavor().getFavor(deity);
        final long curFavor = level.getFavor();
        final long nextFavor = level.getFavorToNextLevel();
        int startY = topPos + SUMMARY_Y;
        // draw "Patron" text
        Optional<ResourceLocation> patron = getMenu().getFavor().getPatron();
        if(patron.isPresent() && patron.get().equals(this.deity)) {
            this.font.draw(matrixStack, new TranslationTextComponent("favor.patron")
                            .withStyle(TextFormatting.WHITE),
                    leftPos + SUMMARY_X, startY + 1, 0xFFFFFF);
            startY += font.lineHeight * 3 / 2;
        }
        // draw favor amounts
        this.font.draw(matrixStack, new TranslationTextComponent("favor.favor").withStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                leftPos + SUMMARY_X, startY, 0xFFFFFF);
        this.font.draw(matrixStack, new StringTextComponent(curFavor + " / " + nextFavor)
                        .withStyle(TextFormatting.DARK_PURPLE),
                leftPos + SUMMARY_X, startY + font.lineHeight * 1 + 1, 0xFFFFFF);
        this.font.draw(matrixStack, new TranslationTextComponent("favor.level").withStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                leftPos + SUMMARY_X, startY + font.lineHeight * 5 / 2, 0xFFFFFF);
        this.font.draw(matrixStack, new StringTextComponent(String.valueOf(level.getLevel() + " / " + (curFavor < 0 ? level.getMin() : level.getMax()))).withStyle(TextFormatting.DARK_PURPLE),
                leftPos + SUMMARY_X, startY + font.lineHeight * 7 / 2 + 1, 0xFFFFFF);
        this.font.draw(matrixStack, new TranslationTextComponent("favor.next_level").withStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                leftPos + SUMMARY_X, startY + font.lineHeight * 5, 0xFFFFFF);
        final boolean capped = level.getLevel() == level.getMin() || level.getLevel() == level.getMax();
        this.font.draw(matrixStack, new StringTextComponent(capped ? "--" : String.valueOf(nextFavor - curFavor)).withStyle(TextFormatting.DARK_PURPLE),
                leftPos + SUMMARY_X, startY + font.lineHeight * 6 + 1, 0xFFFFFF);
        // draw entity
        drawEntityOnScreen(getOrCreateEntity(deity), matrixStack, this.leftPos + PREVIEW_X + 16, this.topPos + PREVIEW_Y + 6, (float) mouseX, (float) mouseY, partialTicks);
    }

    private void renderOfferingsPage(MatrixStack matrixStack) {
        // draw scroll background
        this.minecraft.getTextureManager().bind(SCREEN_WIDGETS);
        this.blit(matrixStack, this.leftPos + SCROLL_X, this.topPos + SCROLL_Y, 188, 0, SCROLL_WIDTH, SCROLL_HEIGHT);
    }

    private void renderSacrificesPage(MatrixStack matrixStack) {
        // draw scroll background
        this.minecraft.getTextureManager().bind(SCREEN_WIDGETS);
        this.blit(matrixStack, this.leftPos + SCROLL_X, this.topPos + SCROLL_Y, 188, 0, SCROLL_WIDTH, SCROLL_HEIGHT);
    }

    private void renderPerksPage(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack.pushPose();
        matrixStack.translate(0, 0, 250);
        // draw header frame
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bind(SCREEN_TEXTURE);
        this.blit(matrixStack, this.leftPos + 23, this.topPos + 13, 23, 13, 208, 28);
        // draw favor level
        renderFavorLevel(matrixStack, mouseX, mouseY, partialTicks);
        // re-draw name and favor
        this.font.drawShadow(matrixStack, deityName, this.leftPos + NAME_X, this.topPos + NAME_Y, 0xFFFFFF);
        this.font.draw(matrixStack, deityFavor, this.leftPos + FAVOR_X, this.topPos + FAVOR_Y, 0xFFFFFF);
        // re-draw level buttons
        for(TextButton b : perkLevelButtonMap.values()) {
            b.renderButton(matrixStack, mouseX, mouseY, partialTicks);
        }
        // draw side frames
        this.getMinecraft().getTextureManager().bind(SCREEN_TEXTURE);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, 24, 170);
        this.blit(matrixStack, this.leftPos + 232, this.topPos, 232, 0, 24, 168);
        // draw perk button tooltip
        matrixStack.translate(0, 0, 50);
        for(PerkButton b : perkButtonMap.computeIfAbsent(deity, key -> ImmutableList.of())) {
            if(b.visible && b.isHovered()) {
                b.renderPerkTooltip(matrixStack, mouseX, mouseY);
            }
        }
        matrixStack.popPose();
    }

    private void renderFavorLevel(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // draw favor boundary
        int level = getMenu().getFavor().getFavor(deity).getLevel();
        matrixStack.pushPose();
        float sizeX = Math.min(PERK_BOUNDS_WIDTH, (level + 1) * (PERK_WIDTH + PERK_SPACE_X) + this.dx - 9);
        if(sizeX > 0) {
            float scaleX = sizeX / 8.0F;
            float scaleY = 14.0F / 8.0F;
            matrixStack.scale(scaleX, scaleY, 1);
            matrixStack.translate((this.leftPos + PERK_BOUNDS_X) / scaleX, (this.topPos + PERK_BOUNDS_Y - 14) / scaleY, 0);
            RenderSystem.enableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.5F);
            this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
            // draw stretched texture
            this.blit(matrixStack, 0, 0, 0, 240, 8, 8);
            // draw boundary texture
            matrixStack.scale(1 / scaleX, 1, 1);
            this.blit(matrixStack, Math.round(sizeX), 0, 8, 240, 8, 8);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        matrixStack.popPose();
    }

    /**
     * This method is called whenever a tab or page is changed
     * @param deity the current deity
     */
    public void updateDeity(final ResourceLocation deity) {
        this.deity = deity;
        // update deity name and favor text for header
        deityName = new TranslationTextComponent(Altar.createTranslationKey(deity))
                .withStyle(TextFormatting.WHITE);
        final FavorLevel favorLevel = getMenu().getFavor().getFavor(deity);
        deityFavor = new StringTextComponent(favorLevel.getLevel() + " / " + favorLevel.getMax())
                .withStyle(TextFormatting.DARK_PURPLE);
        // update based on current page
        int scrollIndex;
        scrollVisible = false;
        scrollEnabled = false;
        switch (this.page) {
            case SUMMARY:
                deityTitle = new TranslationTextComponent(Altar.createTranslationKey(deity) + ".title")
                        .withStyle(TextFormatting.BLACK, TextFormatting.ITALIC);
                break;
            case OFFERINGS:
                int offeringSize = offeringMap.getOrDefault(deity, ImmutableList.of()).size();
                int tradeSize = tradeMap.getOrDefault(deity, ImmutableList.of()).size();
                offeringCount = Math.min(OFFERING_COUNT, offeringSize);
                tradeCount = Math.min(TRADE_COUNT, tradeSize);
                // show or hide buttons based on size of associated lists
                scrollIndex = (int) Math.floor(scrollButton.getScrollAmount() * OFFERING_COUNT / 2);
                for(int i = 0; i < OFFERING_COUNT; i++) {
                    offeringButtons[i].updateOffering(deity, scrollIndex);
                }
                for(int i = 0; i < TRADE_COUNT; i++) {
                    tradeButtons[i].visible = (i < tradeCount);
                    tradeButtons[i].updateOffering(deity, scrollIndex);
                }
                // title
                offeringTitle.visible = true;
                tradeTitle.visible = tradeSize > 0;
                // scroll
                scrollVisible = true;
                scrollEnabled = (offeringSize > offeringCount) || (tradeSize > tradeCount);
                break;
            case SACRIFICES:
                int sacrificeSize = sacrificeMap.getOrDefault(deity, ImmutableList.of()).size();
                sacrificeCount = Math.min(SACRIFICE_COUNT, sacrificeSize);
                // show or hide buttons based on size of associated lists
                scrollIndex = (int) Math.floor(scrollButton.getScrollAmount() * SACRIFICE_COUNT);
                for(int i = 0; i < SACRIFICE_COUNT; i++) {
                    sacrificeButtons[i].updateSacrifice(deity, scrollIndex);
                }
                // title
                sacrificeTitle.visible = true;
                // scroll
                scrollVisible = true;
                scrollEnabled = (sacrificeSize > sacrificeCount);
                break;
            case PERKS:
                // show or hide perks
                for(Map.Entry<ResourceLocation, List<PerkButton>> entry : perkButtonMap.entrySet()) {
                    boolean showPerk = entry.getKey().equals(deity);
                    for(PerkButton b : entry.getValue()) {
                        b.setEnabled(showPerk);
                    }
                }
                // show or hide favor levels
                int favorMin = favorLevel.getMin();
                int favorMax = favorLevel.getMax();
                for(Map.Entry<Integer, TextButton> entry : perkLevelButtonMap.entrySet()) {
                    entry.getValue().setEnabled(entry.getKey() >= favorMin && entry.getKey() <= favorMax);
                }
                // re-center perks and re-calculate which ones are visible
                resetMove();
                break;
        }
        // update scroll bar
        scrollButton.resetScroll();
        scrollButton.visible = scrollVisible;
    }

    public void updateTabGroup(final int tabGroup) {
        this.tabGroup = MathHelper.clamp(tabGroup, 0, tabGroupCount - 1);
        // show or hide tab arrows
        leftButton.visible = (this.tabGroup > 0);
        rightButton.visible = (this.tabGroup < tabGroupCount - 1);
        // update tabs
        for(TabButton tab : tabButtons) {
            if(tab != null) {
                tab.updateDeity();
            }
        }
    }

    public void updateTab(final int tab) {
        this.tab = tab % tabCount;
        updateDeity(deityList.get(this.tabGroup * tabCount + this.tab));
    }

    public void updatePage(final Page page) {
        this.page = page;
        // update page-dependent items
        boolean page0 = page == Page.SUMMARY;
        boolean page1 = page == Page.OFFERINGS;
        boolean page2 = page == Page.SACRIFICES;
        boolean page3 = page == Page.PERKS;
        // summary page
        // nothing
        // offering page
        offeringTitle.visible = page1;
        tradeTitle.visible = page1 && tradeCount > 0;
        for(OfferingButton b : offeringButtons) {
            b.visible = page1;
        }
        for(OfferingButton b : tradeButtons) {
            b.visible = page1;
        }
        // sacrifice page
        sacrificeTitle.visible = page2;
        for(SacrificeButton b : sacrificeButtons) {
            b.visible = page2;
        }
        // perk page
        for(List<PerkButton> l : perkButtonMap.values()) {
            for(PerkButton b : l) {
                b.visible = page3;
            }
        }
        for(TextButton b : perkLevelButtonMap.values()) {
            b.visible = page3;
        }
        // update deity-dependent items
        updateDeity(this.deity);
    }

    /**
     * Updates all scrollable items based on the amount
     * @param amount the scroll percentage in the range [0.0, 1.0]
     */
    public void updateScroll(final float amount) {
        int scrollIndex;
        switch (this.page) {
            case OFFERINGS:
                // scroll index is some discrete value between 0 and the greater of (offeringCount/2, tradeCount)
                scrollIndex = (int) Math.floor(amount * Math.max(offeringCount / 2, tradeCount));
                // update all buttons with the calculated scroll index
                for(int i = 0; i < OFFERING_COUNT; i++) {
                    offeringButtons[i].updateOffering(deity, scrollIndex);
                }
                for(int i = 0; i < TRADE_COUNT; i++) {
                    tradeButtons[i].updateOffering(deity, scrollIndex);
                }
                break;
            case SACRIFICES:
                // scroll index is some discrete between 0 and sacrificeCount
                scrollIndex = (int) Math.floor(amount * sacrificeCount);
                // update all buttons with the calculated scroll index
                for(int i = 0; i < SACRIFICE_COUNT; i++) {
                    sacrificeButtons[i].updateSacrifice(deity, scrollIndex);
                }
                break;
            case SUMMARY:
                break;
            case PERKS:
                break;
        }
    }

    public void resetMove() {
        updateMove(-dx, -dy);
        dx = 0;
        dy = 0;
    }

    public void updateMove(int moveX, int moveY) {
        // determine movement bounds
        FavorLevel level = getMenu().getFavor().getFavor(deity);
        int minX = -(level.getMax() - 5) * (PERK_WIDTH + PERK_SPACE_X);
        int maxX = -(level.getMin() - 2) * (PERK_WIDTH + PERK_SPACE_Y);
        int minY = 0;
        int maxY = 0;
        for(List<Perk> l : perkMap.getOrDefault(deity, ImmutableMap.of()).values()) {
            if(l.size() > minY) {
                minY = l.size();
            }
        }
        minY = -Math.max(0, minY - 4) * (PERK_HEIGHT + PERK_SPACE_Y);
        // update move amounts so they are clamped at bounds
        if(dx + moveX > maxX) moveX = maxX - this.dx;
        if(dx + moveX < minX) moveX = this.dx - minX;
        if(dy + moveY > maxY) moveY = maxY - this.dy;
        if(dy + moveY < minY) moveY = this.dy - minY;
        // track change in movement
        this.dx += moveX;
        this.dy += moveY;
        // move perk buttons
        for(List<PerkButton> l : perkButtonMap.values()) {
            for(PerkButton b : l) {
                b.move(moveX, moveY);
            }
        }
        // move text buttons
        for(TextButton b : perkLevelButtonMap.values()) {
            b.move(moveX);
        }
    }

    public AltarEntity getOrCreateEntity(final ResourceLocation deity) {
        if(!entityMap.containsKey(deity)) {
            AltarEntity altarEntity = AltarEntity.createAltar(inventory.player.level, BlockPos.ZERO, Direction.SOUTH, deity);
            altarEntity.setNoGravity(true);
            altarEntity.noPhysics = true;
            altarEntity.ignoreExplosion();
            altarEntity.setInvulnerable(true);
            entityMap.put(deity, altarEntity);
        }
        AltarEntity entity = entityMap.get(deity);
        entity.setPos(0, 0, 0);
        return entity;
    }

    @SuppressWarnings("deprecation")
    public void drawEntityOnScreen(final AltarEntity entity, final MatrixStack matrixStackIn, final int posX, final int posY,
                                   final float mouseX, final float mouseY, final float partialTicks) {
        float margin = 12;
        float scale = PREVIEW_WIDTH - margin * 2;
        float rotX = (float) Math.atan((double) ((mouseX - this.leftPos) / 40.0F));
        float rotY = (float) Math.atan((double) ((mouseY - this.topPos - PREVIEW_HEIGHT / 2) / 40.0F));

        // Render the Entity with given scale
        RenderSystem.pushMatrix();
        RenderSystem.enableRescaleNormal();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.translatef(posX + margin, posY + margin, 100.0F + 10.0F);
        RenderSystem.translatef(0.0F, PREVIEW_HEIGHT - margin * 1.75F, 0.0F);
        //RenderSystem.rotatef(this.blockRotation.getOpposite().getHorizontalAngle(), 0.0F, -1.0F, 0.0F);
        RenderSystem.scalef(1.0F, -1.0F, 1.0F);
        RenderSystem.scalef(scale, scale, scale);
        RenderSystem.rotatef(rotX * 15.0F, 0.0F, 1.0F, 0.0F);
        RenderSystem.rotatef(rotY * 15.0F, 1.0F, 0.0F, 0.0F);

        RenderHelper.setupForFlatItems();

        IRenderTypeBuffer.Impl bufferType = minecraft.renderBuffers().bufferSource();
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity)
                .render(entity, 0F, partialTicks, matrixStackIn, bufferType, 15728880);
        bufferType.endBatch();

        RenderSystem.enableDepthTest();
        RenderHelper.setupFor3DItems();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
        RenderSystem.popMatrix();
    }
    /**
     * Renders a scroll background for a tooltip with the given position and size
     * @param matrixStack the render stack
     * @param startX the x position of upper left corner
     * @param startY the y position of upper left corner
     * @param sizeX the width of the scroll
     * @param sizeY the height of the scroll
     */
    private void renderPerkTooltipBackground(MatrixStack matrixStack, final float startX, final float startY,
                                             float sizeX, float sizeY) {
        // minimum size of tooltip
        sizeX = Math.max(42, sizeX);
        sizeY = Math.max(50, sizeY);
        // u and v coordinates of background
        int u = 31;
        int v = 132;
        // corner pieces (width and height)
        int cWidth = 16;
        int cHeight = 20;
        // middle piece (width and height)
        int mWidth = 8;
        int mHeight = 8;
        // local x and y scale
        float scaleX = (sizeX - (cWidth * 2)) / (float)mWidth;
        float scaleY = (sizeY - (cHeight * 2)) / (float)mHeight;
        matrixStack.pushPose();
        matrixStack.translate(startX, startY, 0);
        // prepare to render
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
        // draw upper left corner
        this.blit(matrixStack, 0, 0, u, v, cWidth, cHeight);
        // draw upper middle
        matrixStack.scale(scaleX, 1, 1);
        matrixStack.translate(cWidth / scaleX, 0, 0);
        this.blit(matrixStack, 0, 0, u + cWidth, v, mWidth, cHeight);
        // draw upper right
        matrixStack.scale(1 / scaleX, 1, 1);
        matrixStack.translate(mWidth * scaleX, 0, 0);
        this.blit(matrixStack, 0, 0, u + cWidth + mWidth, v, cWidth, cHeight);
        // draw middle left
        matrixStack.translate(-(cWidth + (mWidth * scaleX)), cHeight, 0);
        matrixStack.scale(1, scaleY, 1);
        this.blit(matrixStack, 0, 0, u, v + cHeight, cWidth, mHeight);
        // draw middle
        matrixStack.translate(cWidth, 0, 0);
        matrixStack.scale(scaleX, 1, 1);
        this.blit(matrixStack, 0, 0, u + cWidth, v + cHeight, mWidth, mHeight);
        // draw middle right
        matrixStack.scale(1 / scaleX, 1, 1);
        matrixStack.translate(mWidth * scaleX, 0, 0);
        this.blit(matrixStack, 0, 0, u + cWidth + mWidth, v + cHeight, cWidth, mHeight);
        // draw lower left
        matrixStack.scale(1, 1 / scaleY, 1);
        matrixStack.translate(-(cWidth + (mWidth * scaleX)), (mHeight * scaleY), 0);
        this.blit(matrixStack, 0, 0, u, v + cHeight + mHeight, cWidth, cHeight);
        // draw lower middle
        matrixStack.scale(scaleX, 1, 1);
        matrixStack.translate(cWidth / scaleX, 0, 0);
        this.blit(matrixStack, 0, 0, u + cWidth, v + cHeight + mHeight, mWidth, cHeight);
        // draw upper right
        matrixStack.scale(1 / scaleX, 1, 1);
        matrixStack.translate(mWidth * scaleX, 0, 0);
        this.blit(matrixStack, 0, 0, u + cWidth + mWidth, v + cHeight + mHeight, cWidth, cHeight);
        matrixStack.popPose();
    }

    private void renderStrikethrough(final MatrixStack matrixStack, final int x, final int y, final int width) {
        matrixStack.pushPose();
        float scale = (float)width / 18.0F;
        matrixStack.scale(scale, 1, 1);
        matrixStack.translate(x / scale, y, this.itemRenderer.blitOffset + 101);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
        this.blit(matrixStack, 0,0, 0, 250, 18, 2);
        matrixStack.popPose();
    }

    protected class PerkButton extends Button {

        private Perk perk;
        private List<ITextComponent> perkActions;
        private List<ITextComponent> perkConditions;
        private ITextComponent perkChance;
        private ITextComponent perkRange;
        private boolean enabled;
        private int tooltipWidth;

        public PerkButton(final FavorScreen gui, final Perk perk, int x, int y) {
            super(x, y, PERK_WIDTH, PERK_HEIGHT, StringTextComponent.EMPTY, b -> {});
            this.perkActions = new ArrayList<>();
            this.perkConditions = new ArrayList<>();
            this.setPerk(perk);
            setEnabled(true);
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && this.enabled && perk != null) {
                int blitY = 196;
                if(perk.getIcon().isFancy()) {
                    blitY += PERK_HEIGHT;
                }
                // draw color
                RenderSystem.color4f(perk.getIcon().getColorRed(), perk.getIcon().getColorGreen(), perk.getIcon().getColorBlue(), 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, 0, blitY, PERK_WIDTH, PERK_HEIGHT);
                // draw bg
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.blit(matrixStack, this.x, this.y, 22, blitY, PERK_WIDTH, PERK_HEIGHT);
                // draw item
                FavorScreen.this.itemRenderer.renderGuiItem(perk.getIcon().getItem(), this.x + (PERK_WIDTH - 16) / 2, this.y + (PERK_HEIGHT - 16) / 2);
                // draw cooldown
                long timeElapsed = inventory.player.level.getGameTime() - openTimestamp;
                long cooldown = getMenu().getFavor().getPerkCooldown(this.perk.getCategory()) - timeElapsed;
                // render cooldown texture on top of item
                if(cooldown > 0 && perk.getCooldown() > 0) {
                    matrixStack.pushPose();
                    matrixStack.translate(0, 0, FavorScreen.this.itemRenderer.blitOffset + 110);
                    // determine v offset
                    int vOffset = Math.round((MathHelper.clamp(1.0F - (float)cooldown / (float)perk.getCooldown(), 0.0F, 1.0F)) * PERK_HEIGHT);
                    // draw cooldown
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.5F);
                    FavorScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                    this.blit(matrixStack, this.x, this.y + vOffset, 44, blitY + vOffset, PERK_WIDTH, PERK_HEIGHT - vOffset);
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                    matrixStack.popPose();
                }
            }
        }

        public void setPerk(final Perk perk) {
            this.perk = perk;
            this.perkActions.clear();
            this.perkConditions.clear();
            this.perkChance = StringTextComponent.EMPTY;
            this.perkRange = StringTextComponent.EMPTY;
            boolean isRandomPerk = false;
            if(perk != null) {
                // add all perk action titles
                for(PerkAction data : perk.getActions()) {
                    perkActions.add(data.getDisplayName().copy().withStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE));
                    perkActions.add(data.getDisplayDescription().copy().withStyle(TextFormatting.BLUE));
                }
                // add perk condition texts
                for(PerkCondition condition : perk.getConditions()) {
                    // do not show "random tick" conditions
                    if(condition.getType() != PerkCondition.Type.RANDOM_TICK) {
                        perkConditions.add(condition.getDisplayName().copy().withStyle(TextFormatting.DARK_GRAY));
                    } else {
                        isRandomPerk = true;
                    }
                }
                // add prefix to each condition based on plurality
                if (perkConditions.size() > 0) {
                    // add prefix to first condition
                    ITextComponent t2 = new TranslationTextComponent("favor.perk.condition.single", perkConditions.get(0))
                            .withStyle(TextFormatting.DARK_GRAY);
                    perkConditions.set(0, t2);
                    // add prefix to following conditions
                    for (int i = 1, l = perkConditions.size(); i < l; i++) {
                        t2 = new TranslationTextComponent("favor.perk.condition.multiple", perkConditions.get(i))
                                .withStyle(TextFormatting.DARK_GRAY);
                        perkConditions.set(i, t2);
                    }
                }
                // add text to display favor range
                FavorLevel favorLevel = FavorScreen.this.getMenu().getFavor().getFavor(perk.getDeity());
                TextFormatting color = perk.getRange().isInRange(favorLevel.getLevel()) ? TextFormatting.DARK_GREEN : TextFormatting.RED;
                if(perk.getRange().getMaxLevel() == favorLevel.getMax()) {
                    perkRange = new TranslationTextComponent("gui.favor.perk.range.above_min", perk.getRange().getMinLevel()).withStyle(color);
                } else if(perk.getRange().getMinLevel() == favorLevel.getMin()) {
                    perkRange = new TranslationTextComponent("gui.favor.perk.range.below_max", perk.getRange().getMaxLevel()).withStyle(color);
                } else {
                    perkRange = new TranslationTextComponent("gui.favor.perk.range.between",
                            perk.getRange().getMinLevel(), perk.getRange().getMaxLevel()).withStyle(color);
                }
                // determine perk chance
                float chance = MathHelper.clamp(perk.getAdjustedChance(favorLevel), 0.0F, 1.0F);
                if(isRandomPerk) {
                    chance *= RPGGods.CONFIG.getRandomPerkChance();
                }
                // add perk chance (formatted to 2 or fewer decimal places)
                if(chance < 0.999999F) {
                    String chanceString = String.format("%.2f", chance * 100.0F)
                            .replaceAll("0*$", "")
                            .replaceAll("\\.$", "");
                    perkChance = new TranslationTextComponent("gui.favor.perk.chance", chanceString)
                            .withStyle(TextFormatting.BLACK);
                }
            }
            this.tooltipWidth = calculateWidth();
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void move(final int moveX, final int moveY) {
            this.x += moveX;
            this.y += moveY;
            this.visible = this.enabled &&
                    !( this.x > FavorScreen.this.leftPos + PERK_BOUNDS_X + PERK_BOUNDS_WIDTH
                    || this.x < FavorScreen.this.leftPos + PERK_BOUNDS_X - this.width
                    || this.y > FavorScreen.this.topPos + PERK_BOUNDS_Y + PERK_BOUNDS_HEIGHT
                    || this.y < FavorScreen.this.topPos + PERK_BOUNDS_Y - this.height);
        }

        public void renderPerkTooltip(MatrixStack matrixStack, final int mouseX, final int mouseY) {
            int margin = 14;
            // determine start coordinates
            int startX = mouseX + 10;
            if(startX + tooltipWidth + margin > FavorScreen.this.width) {
                startX = FavorScreen.this.width - tooltipWidth - margin - 10;
            }
            int startY = mouseY - PERK_TOOLTIP_HEIGHT / 2;
            final int lineHeight = 11;
            // determine background size
            int lines = perkActions.size() + perkConditions.size() + 6;
            if(perkChance.equals(StringTextComponent.EMPTY)) {
                lines--;
            }
            renderPerkTooltipBackground(matrixStack, startX, startY, tooltipWidth + margin, lines * lineHeight + 6);
            startX += margin / 2;
            startY += 14;
            // draw perk data
            int line = 0;
            // draw action(s)
            for (ITextComponent t : perkActions) {
                FavorScreen.this.font.draw(matrixStack, t, startX, startY + lineHeight * (line++), 0xFFFFFF);
            }
            // line space
            startY += 5;
            // draw conditions
            for (ITextComponent t : perkConditions) {
                FavorScreen.this.font.draw(matrixStack, t, startX, startY + lineHeight * (line++), 0xFFFFFF);
            }
            if(!perkConditions.isEmpty()) {
                startY += 5;
            }
            // draw chance
            if(!perkChance.equals(StringTextComponent.EMPTY)) {
                FavorScreen.this.font.draw(matrixStack, perkChance, startX, startY + lineHeight * (line++), 0xFFFFFF);
                // line space
                startY += 5;
            }
            // draw range
            ITextComponent unlock = new TranslationTextComponent("gui.favor.perk.unlock")
                    .withStyle(TextFormatting.BLACK);
            FavorScreen.this.font.draw(matrixStack, unlock, startX, startY + lineHeight * (line++), 0xFFFFFF);
            FavorScreen.this.font.draw(matrixStack, perkRange, startX, startY + lineHeight * (line++), 0xFFFFFF);
        }

        /**
         * Iterates through all text components attached to this button's tooltip
         * @return the maximum width of this button's text components
         */
        private int calculateWidth() {
            int maxWidth = 0;
            for(ITextComponent t : perkActions) {
                maxWidth = Math.max(maxWidth, FavorScreen.this.font.width(t));
            }
            for(ITextComponent t : perkConditions) {
                maxWidth = Math.max(maxWidth, FavorScreen.this.font.width(t));
            }
            maxWidth = Math.max(maxWidth, FavorScreen.this.font.width(perkChance));
            maxWidth = Math.max(maxWidth, FavorScreen.this.font.width(perkRange));
            return maxWidth;
        }
    }

    protected class TradeButton extends OfferingButton {
        protected static final int ARROW_WIDTH = 12;
        protected static final int ARROW_HEIGHT = 9;
        protected ITextComponent tradeFunctionText;
        protected ITextComponent unlockText;
        protected ITextComponent unlockTooltip;
        protected boolean hasTrade;

        public TradeButton(FavorScreen gui, int index, int x, int y) {
            super(gui, index, x, y, TRADE_WIDTH, OFFERING_HEIGHT);
            tradeFunctionText = new TranslationTextComponent("?")
                    .withStyle(TextFormatting.BOLD, TextFormatting.DARK_BLUE);
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && offering != null) {
                // draw item
                FavorScreen.this.itemRenderer.renderGuiItem(offering.getAccept(), this.x, this.y);
                FavorScreen.this.itemRenderer.renderGuiItemDecorations(FavorScreen.this.font, offering.getAccept(), this.x, this.y);
                // draw trade
                if(hasTrade) {
                    // draw trade item
                    FavorScreen.this.itemRenderer.renderGuiItem(offering.getTrade().get(), this.x + 18 + ARROW_WIDTH, this.y);
                    FavorScreen.this.itemRenderer.renderGuiItemDecorations(FavorScreen.this.font, offering.getTrade().get(), this.x + 18 + ARROW_WIDTH, this.y);
                } else if(offering.getFunction().isPresent()) {
                    // draw question mark instead of item
                    FavorScreen.this.font.draw(matrixStack, tradeFunctionText, this.x + 18 + ARROW_WIDTH + 4, this.y + textY, 0xFFFFFF);
                    // draw function text
                    FavorScreen.this.font.draw(matrixStack, functionText, this.x + 18 * 3 + ARROW_WIDTH - 4, this.y + textY, 0xFFFFFF);
                }
                // draw unlock text
                FavorScreen.this.font.draw(matrixStack, unlockText, this.x + 18 * 2 + ARROW_WIDTH + 4, this.y + textY, 0xFFFFFF);
                // draw arrow
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x + 18, this.y + textY, 113, 130, ARROW_WIDTH, ARROW_HEIGHT);
                // draw strikethrough
                long timeElapsed = inventory.player.level.getGameTime() - openTimestamp;
                if(this.cooldown - timeElapsed > 1) {
                    FavorScreen.this.renderStrikethrough(matrixStack, this.x, this.y + this.height / 2, this.width - 2);
                }
            }
        }

        @Override
        public void updateOffering(final ResourceLocation deity, final int startIndex) {
            final int offeringId = startIndex * 2 + id;
            final List<ImmutablePair<ResourceLocation, Offering>> offerings = FavorScreen.this.tradeMap.getOrDefault(deity, ImmutableList.of());
            if(offeringId < offerings.size()) {
                this.visible = true;
                ImmutablePair<ResourceLocation, Offering> tuple = offerings.get(offeringId);
                updateOffering(tuple.getLeft(), tuple.getRight());
            } else {
                this.visible = false;
            }
        }

        @Override
        protected void updateOffering(final ResourceLocation offeringId, final Offering offering) {
            super.updateOffering(offeringId, offering);
            this.hasTrade = offering.getTrade().isPresent() && !offering.getTrade().get().isEmpty();
            // determine item tooltip
            if(offering.getTrade().isPresent()) {
                this.unlockText = new StringTextComponent("" + offering.getTradeMinLevel()).withStyle(TextFormatting.DARK_PURPLE);
                this.unlockTooltip = new TranslationTextComponent("gui.favor.offering.unlock.tooltip", offering.getTradeMinLevel());
            }
        }

        @Override
        protected List<ITextComponent> getTooltip(final int mouseX, final int mouseY) {
            if(offering != null) {
                if(offering.getFunction().isPresent() && mouseX >= (this.x + 18 * 3 + ARROW_WIDTH - 4)) {
                    return Lists.newArrayList(functionTooltip);
                }
                if(mouseX >= (this.x + 18 * 2 + ARROW_WIDTH) && mouseX <= (this.x + 18 * 3 + ARROW_WIDTH - 4)) {
                    return Lists.newArrayList(unlockTooltip);
                }
                if(offering.getTrade().isPresent() && mouseX >= (this.x + 18 + ARROW_WIDTH) && mouseX <= (this.x + 18 * 2 + ARROW_WIDTH)) {
                    if(offering.getTrade().get().isEmpty() && offering.getFunction().isPresent()) {
                        return Lists.newArrayList(functionTooltip);
                    }
                    return FavorScreen.this.getTooltipFromItem(offering.getTrade().get());
                }
                if(mouseX <= (this.x + 18)) {
                    return FavorScreen.this.getTooltipFromItem(offering.getAccept());
                }
            }
            return Lists.newArrayList();
        }
    }

    protected class OfferingButton extends Button {
        protected final int textY = 5;
        protected int id;
        protected Offering offering;
        protected long cooldown;
        protected ITextComponent favorText;
        protected final ITextComponent functionText;
        protected ITextComponent functionTooltip;

        public OfferingButton(final FavorScreen gui, final int index, final int x, final int y) {
            this(gui, index, x, y, OFFERING_WIDTH, OFFERING_HEIGHT);
        }

        public OfferingButton(final FavorScreen gui, final int index, int x, int y,final int width, final int height) {
            super(x, y, width, height, StringTextComponent.EMPTY, b -> {},
                    (b, m, bx, by) -> gui.renderWrappedToolTip(m, ((OfferingButton)b).getTooltip(bx, by), bx, by, gui.font));
            this.id = index;
            this.favorText = StringTextComponent.EMPTY;
            this.functionText = new StringTextComponent(" \u2605 ").withStyle(TextFormatting.BLUE);
            this.functionTooltip = StringTextComponent.EMPTY;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && offering != null) {
                // draw item
                FavorScreen.this.itemRenderer.renderGuiItem(offering.getAccept(), this.x, this.y);
                FavorScreen.this.itemRenderer.renderGuiItemDecorations(FavorScreen.this.font, offering.getAccept(), this.x, this.y);
                // draw favor text
                FavorScreen.this.font.draw(matrixStack, favorText, this.x + 18, this.y + textY, 0xFFFFFF);
                // draw function text
                if(offering != null && offering.getFunction().isPresent()) {
                    FavorScreen.this.font.draw(matrixStack, functionText, this.x + 18 * 2 - 2, this.y + textY, 0xFFFFFF);
                }
                // draw strikethrough
                long timeElapsed = inventory.player.level.getGameTime() - openTimestamp;
                if(this.cooldown - timeElapsed > 1) {
                    FavorScreen.this.renderStrikethrough(matrixStack, this.x, this.y + this.height / 2, this.width - 4);
                }
            }
        }

        public void updateOffering(final ResourceLocation deity, final int startIndex) {
            final int offeringId = startIndex * 2 + id;
            final List<ImmutablePair<ResourceLocation, Offering>> offerings = FavorScreen.this.offeringMap.getOrDefault(deity, ImmutableList.of());
            if(offeringId < offerings.size()) {
                this.visible = true;
                ImmutablePair<ResourceLocation, Offering> tuple = offerings.get(offeringId);
                updateOffering(tuple.getLeft(), tuple.getRight());
            } else {
                this.visible = false;
            }
        }

        protected void updateOffering(final ResourceLocation offeringId, final Offering offering) {
            this.offering = offering;
            this.cooldown = FavorScreen.this.getMenu().getFavor().getOfferingCooldown(offeringId).getCooldown();
            // determine favor text
            int favorAmount = offering.getFavor();
            String favorString;
            TextFormatting color;
            if(favorAmount >= 0) {
                favorString = "+" + favorAmount;
                color = TextFormatting.DARK_GREEN;
            } else {
                favorString = "" + favorAmount;
                color = TextFormatting.DARK_RED;
            }
            this.favorText = new StringTextComponent(favorString).withStyle(color);
            if(offering.getFunctionText().isPresent()) {
                this.functionTooltip = new TranslationTextComponent(offering.getFunctionText().get());
            } else {
                this.functionTooltip = new TranslationTextComponent("gui.favor.offering.function.tooltip");
            }
        }

        protected List<ITextComponent> getTooltip(final int mouseX, final int mouseY) {
            if(offering != null && offering.getFunction().isPresent() && mouseX >= (this.x + 18 * 2 - 2)) {
                return Lists.newArrayList(functionTooltip);
            }
            if(mouseX <= (this.x + 18)) {
                return FavorScreen.this.getTooltipFromItem(this.offering.getAccept());
            }
            return Lists.newArrayList();
        }
    }

    protected class SacrificeButton extends Button {
        protected int id;
        protected Sacrifice sacrifice;
        protected long cooldown;
        protected ITextComponent entityText;
        protected ITextComponent favorText;
        protected final ITextComponent functionText;
        protected ITextComponent functionTooltip;

        public SacrificeButton(final FavorScreen gui, final int index, int x, int y) {
            super(x, y, SACRIFICE_WIDTH, SACRIFICE_HEIGHT, StringTextComponent.EMPTY, b -> {},
                    (b, m, bx, by) -> ((SacrificeButton)b).getTooltip(bx, by).ifPresent(t -> gui.renderTooltip(m, t, bx, by)));
            this.id = index;
            this.entityText = StringTextComponent.EMPTY;
            this.favorText = StringTextComponent.EMPTY;
            this.functionText = new StringTextComponent(" \u2605 ").withStyle(TextFormatting.BLUE);
            this.functionTooltip = StringTextComponent.EMPTY;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && sacrifice != null) {
                // draw entity text
                FavorScreen.this.font.draw(matrixStack, entityText, this.x, this.y, 0xFFFFFF);
                // draw favor text
                FavorScreen.this.font.draw(matrixStack, favorText, this.x + 18 * 7, this.y, 0xFFFFFF);
                // draw function text
                if(sacrifice != null && sacrifice.getFunction().isPresent()) {
                    FavorScreen.this.font.draw(matrixStack, functionText, this.x + 18 * 8, this.y, 0xFFFFFF);
                }
                // draw strikethrough
                long timeElapsed = inventory.player.level.getGameTime() - openTimestamp;
                if(this.cooldown - timeElapsed > 1) {
                    FavorScreen.this.renderStrikethrough(matrixStack, this.x, this.y + this.height / 2, this.width - 2);
                }
            }
        }

        public void updateSacrifice(final ResourceLocation deity, final int startIndex) {
            final int sacrificeId = startIndex * 2 + id;
            final List<ImmutablePair<ResourceLocation, Sacrifice>> sacrifices = FavorScreen.this.sacrificeMap.getOrDefault(deity, ImmutableList.of());
            if(sacrificeId < sacrifices.size()) {
                this.visible = true;
                ImmutablePair<ResourceLocation, Sacrifice> tuple = sacrifices.get(sacrificeId);
                updateSacrifice(tuple.getLeft(), tuple.getRight());
            } else {
                this.visible = false;
            }
        }

        protected void updateSacrifice(final ResourceLocation sacrificeId, final Sacrifice sacrifice) {
            this.sacrifice = sacrifice;
            this.cooldown = FavorScreen.this.getMenu().getFavor().getSacrificeCooldown(sacrificeId).getCooldown();
            // determine entity text
            EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(sacrifice.getEntity());
            if(entityType != null) {
                this.entityText = new TranslationTextComponent(entityType.getDescriptionId()).withStyle(TextFormatting.BLACK);
            }
            // determine favor text
            int favorAmount = sacrifice.getFavor();
            String favorString;
            TextFormatting color;
            if(favorAmount >= 0) {
                favorString = "+" + favorAmount;
                color = TextFormatting.DARK_GREEN;
            } else {
                favorString = "" + favorAmount;
                color = TextFormatting.DARK_RED;
            }
            this.favorText = new StringTextComponent(favorString).withStyle(color);
            if(sacrifice.getFunctionText().isPresent()) {
                this.functionTooltip = new TranslationTextComponent(sacrifice.getFunctionText().get());
            } else {
                this.functionTooltip = new TranslationTextComponent("gui.favor.sacrifice.function.tooltip");
            }
        }

        protected Optional<ITextComponent> getTooltip(final int mouseX, final int mouseY) {
            if(sacrifice != null && sacrifice.getFunction().isPresent() && mouseX > (this.x + 18 * 8)) {
                return Optional.of(functionTooltip);
            }
            return Optional.empty();
        }
    }

    protected class TextButton extends Button {

        private boolean enabled;

        public TextButton(final Screen gui, int x, int y, int width, int height, ITextComponent title) {
            super(x, y, width, height, title, b -> {});
            this.setEnabled(true);
        }

        public TextButton(final Screen gui, int x, int y, int width, int height, ITextComponent title, ITextComponent tooltip) {
            super(x, y, width, height, title, b -> {}, tooltip == null ? (b, m, bx, by) -> {} :
                    (b, m, bx, by) -> gui.renderTooltip(m, tooltip, bx, by));
            this.setEnabled(true);
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && this.enabled) {
                // draw text
                FavorScreen.this.font.draw(matrixStack, getMessage(), this.x, this.y, 0xFFFFFF);
            }
        }

        public void move(final int moveX) {
            this.x += moveX;
            this.visible = this.enabled &&
                    !(this.x > FavorScreen.this.leftPos + PERK_BOUNDS_X + PERK_BOUNDS_WIDTH
                    || this.x < FavorScreen.this.leftPos + PERK_BOUNDS_X - this.width);
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }
    }

    protected class TabButton extends Button {

        private int id;
        private ItemStack item = ItemStack.EMPTY;
        private ResourceLocation deity;

        public TabButton(final FavorScreen gui, final int index, final ITextComponent title, final int x, final int y) {
            super(x, y, TAB_WIDTH, TAB_HEIGHT, title, b -> gui.updateTab(index),
                    (b, m, bx, by) -> gui.renderTooltip(m, b.getMessage(), bx, by));
            this.id = index;
            updateDeity();
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final boolean selected = FavorScreen.this.tab == id && FavorScreen.this.deity.equals(this.deity);
                int dY = selected ? 0 : 4;
                final int u = (id % TAB_COUNT) * TAB_WIDTH;
                final int v = selected ? this.height : dY;
                // draw button background
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, u, v, this.width, this.height - dY);
                // draw item
                FavorScreen.this.itemRenderer.renderGuiItem(item, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
            }
        }

        public void updateDeity() {
            final int deityId = id + (FavorScreen.this.tabGroup * FavorScreen.this.tabCount);
            if(deityId < FavorScreen.this.deityList.size()) {
                this.visible = true;
                this.deity = FavorScreen.this.deityList.get(deityId);
                this.setMessage(new TranslationTextComponent(Altar.createTranslationKey(deity)));
                this.item = RPGGods.DEITY.get(deity).orElse(Deity.EMPTY).getIcon();
            } else {
                this.visible = false;
                this.deity = null;
            }
        }
    }

    protected class TabArrowButton extends Button {
        protected boolean left;

        public TabArrowButton(FavorScreen gui, int x, int y, boolean left) {
            super(x, y, ARROW_WIDTH, ARROW_HEIGHT, StringTextComponent.EMPTY,
                    b -> gui.updateTabGroup(gui.tabGroup + (left ? -1 : 1)));
            this.left = left;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final int u = left ? ARROW_WIDTH : 0;
                final int v = 130 + (isHovered() ? this.height : 0);
                // draw button
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, u, v, this.width, this.height);
            }
        }

    }

    protected class PageButton extends Button {

        private final Page page;
        private final int u;
        private final int v;

        public PageButton(final FavorScreen screenIn, final int x, final int y, final int u, final int v,
                          final ITextComponent title, final ITextComponent tooltip, final Page page) {
            super(x, y, PAGE_WIDTH, PAGE_HEIGHT, title, b -> screenIn.updatePage(page),
                    (b, m, bx, by) -> screenIn.renderTooltip(m, tooltip, bx, by));
            this.page = page;
            this.u = u;
            this.v = v;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final boolean selected = FavorScreen.this.page == this.page;
                int dY = selected ? 0 : -4;
                int uX = (page.ordinal() % PAGE_COUNT) * PAGE_WIDTH;
                int vY = 64 + (selected ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                // draw tab
                this.blit(matrixStack, this.x, this.y - dY, uX, vY, this.width, this.height + dY);
                // draw icon
                this.blit(matrixStack, this.x + (this.width - 18) / 2, this.y + dY + 2 + (this.height - 18) / 2, u, v, 18, 18);
            }
        }
    }

    protected static enum Page {
        SUMMARY("gui.favor.summary"),
        OFFERINGS("gui.favor.offerings"),
        SACRIFICES("gui.favor.sacrifices"),
        PERKS("gui.favor.perks");

        private String title;

        private Page(final String titleIn) {
            title = titleIn;
        }

        public String getTitle() { return title; }
    }
}
