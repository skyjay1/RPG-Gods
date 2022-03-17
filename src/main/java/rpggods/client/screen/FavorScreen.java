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
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.entity.AltarEntity;
import rpggods.favor.FavorLevel;
import rpggods.favor.IFavor;
import rpggods.gui.FavorContainer;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class FavorScreen extends ContainerScreen<FavorContainer> {

    // CONSTANTS
    private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/gui/favor/favor.png");
    private static final ResourceLocation SCREEN_WIDGETS = new ResourceLocation(RPGGods.MODID, "textures/gui/favor/favor_widgets.png");
    private static final ResourceLocation PERK_TOOLTIP = new ResourceLocation(RPGGods.MODID, "textures/gui/favor/perk.png");
    private static final ResourceLocation FAVOR_LEVEL = new ResourceLocation(RPGGods.MODID, "textures/gui/favor/level.png");

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
    private static final int OFFERING_Y = 38;
    private static final int OFFERING_WIDTH = 18 * 3;
    private static final int OFFERING_HEIGHT = 18;
    private static final int TRADE_X = 140;
    private static final int TRADE_Y = OFFERING_Y;
    private static final int TRADE_WIDTH = 18 * 4;
    private static final int OFFERING_COUNT = 14;
    private static final int TRADE_COUNT = 7;

    // Sacrifices page
    private static final int SACRIFICE_X = 32;
    private static final int SACRIFICE_Y = 40;
    private static final int SACRIFICE_WIDTH = 18 * 9;
    private static final int SACRIFICE_HEIGHT = 16;
    private static final int SACRIFICE_COUNT = 8;

    // Perks page
    private static final int PERK_WIDTH = 22;
    private static final int PERK_HEIGHT = 22;
    private static final int PERK_SPACE_X = 6;
    private static final int PERK_SPACE_Y = 2;
    private static final int PERK_TOOLTIP_WIDTH = 101;
    private static final int PERK_TOOLTIP_HEIGHT = 122;
    private static final int PERK_BOUNDS_X = 23;
    private static final int PERK_BOUNDS_Y = 41;
    private static final int PERK_BOUNDS_WIDTH = 209;
    private static final int PERK_BOUNDS_HEIGHT = SCREEN_HEIGHT - PERK_BOUNDS_Y - PERK_HEIGHT;

    // Data
    private static final List<ResourceLocation> deityList = new ArrayList<>();
    private static final Map<ResourceLocation, AltarEntity> entityMap = new HashMap<>();
    private static final Map<ResourceLocation, List<Offering>> offeringMap = new HashMap();
    private static final Map<ResourceLocation, List<Offering>> tradeMap = new HashMap();
    private static final Map<ResourceLocation, List<Sacrifice>> sacrificeMap = new HashMap();
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
        this.xSize = SCREEN_WIDTH;
        this.ySize = SCREEN_HEIGHT;
        this.openTimestamp = inv.player.world.getGameTime();
        // add all deities to list
        final IFavor favor = screenContainer.getFavor();
        deityList.clear();
        for(Map.Entry<ResourceLocation, Optional<Altar>> entry : RPGGods.ALTAR.getEntries()) {
            if(entry.getValue().isPresent() && entry.getValue().get().getDeity().isPresent()) {
                deityList.add(entry.getKey());
            }
        }
        deityList.sort((d1, d2) -> favor.getFavor(d2).compareToAbs(favor.getFavor(d1)));
        // add all offerings to map
        offeringMap.clear();
        tradeMap.clear();
        for(Optional<Offering> optional : RPGGods.OFFERING.getValues()) {
            optional.ifPresent(offering -> {
                Map<ResourceLocation, List<Offering>> map = offering.getTrade().isPresent() ? tradeMap : offeringMap;
                map.computeIfAbsent(offering.getDeity(), id -> Lists.newArrayList()).add(offering);
            });
        }
        offeringCount = OFFERING_COUNT;
        tradeCount = TRADE_COUNT;
        // add all sacrifices to map
        sacrificeMap.clear();
        for(Optional<Sacrifice> optional : RPGGods.SACRIFICE.getValues()) {
            optional.ifPresent(sacrifice -> {
                sacrificeMap.computeIfAbsent(sacrifice.getDeity(), id -> Lists.newArrayList()).add(sacrifice);
            });
        }
        sacrificeCount = SACRIFICE_COUNT;
        // add all perks to map
        perkMap.clear();
        Perk perk;
        for(Optional<Perk> optional : RPGGods.PERK.getValues()) {
            if(optional.isPresent() && !optional.get().getIcon().isHidden()) {
                perk = optional.get();
                if(!perkMap.containsKey(perk.getDeity())) {
                    // add map with keys for all levels
                    FavorLevel favorLevel = favor.getFavor(perk.getDeity());
                    Map<Integer, List<Perk>> map = new HashMap<>();
                    for(int i = favorLevel.getMin(), j = favorLevel.getMax(); i <= j; i++) {
                        map.put(i, Lists.newArrayList());
                    }
                    perkMap.put(perk.getDeity(), map);
                }
                // determine which level to place the perk
                int unlock = (perk.getRange().getMaxLevel() <= 0) ? perk.getRange().getMaxLevel() : perk.getRange().getMinLevel();
                // actually add the perk to the map
                perkMap.get(perk.getDeity()).get(unlock).add(perk);
            }
        }
        // determine current page
        if(deityList.size() > 0) {
            deity = screenContainer.getDeity().orElse(deityList.get(0));
        }
        // initialize number of tabs
        tabCount = Math.min(deityList.size(), TAB_COUNT);
        tabGroupCount = 1 + (deityList.size() / TAB_COUNT);
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        this.playerInventoryTitleY = this.height;
        // clear button lists and maps
        perkButtonMap.clear();
        perkLevelButtonMap.clear();
        // add tabs
        int startX = (this.xSize - (TAB_WIDTH * TAB_COUNT)) / 2;
        int startY;
        for(int i = 0; i < tabCount; i++) {
            tabButtons[i] = this.addButton(new TabButton(this, i, new TranslationTextComponent(Altar.createTranslationKey(deityList.get(i))),
                    guiLeft + startX + (i * TAB_WIDTH), guiTop - TAB_HEIGHT + 13));
        }
        // add tab buttons
        leftButton = this.addButton(new TabArrowButton(this, guiLeft + startX - (ARROW_WIDTH + 4), guiTop - TAB_HEIGHT + 20, true));
        rightButton = this.addButton(new TabArrowButton(this, guiLeft + startX + TAB_WIDTH * TAB_COUNT + 4, guiTop - TAB_HEIGHT + 20, false));
        // add pages
        startX = (this.xSize - (PAGE_WIDTH * PAGE_COUNT)) / 2;
        for(int i = 0; i < PAGE_COUNT; i++) {
            FavorScreen.Page p = FavorScreen.Page.values()[i];
            pageButtons[i] = this.addButton(new PageButton(this, guiLeft + startX + (i * PAGE_WIDTH), guiTop + SCREEN_HEIGHT - 7,
                    125 + 18 * i, 130, new TranslationTextComponent(p.getTitle()), new TranslationTextComponent(p.getTitle() + ".tooltip"), p));
        }
        // add scroll bar
        scrollButton = this.addButton(new ScrollButton<>(this, this.guiLeft + SCROLL_X, this.guiTop + SCROLL_Y, SCROLL_WIDTH, SCROLL_HEIGHT,
                0, 165, SCREEN_WIDGETS, true, gui -> gui.scrollEnabled, b -> updateScroll(b.getScrollAmount())));
        // re-usable text components
        ITextComponent text;
        ITextComponent tooltip;
        // add offering UI elements
        text = new TranslationTextComponent("gui.favor.offerings").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.offerings.tooltip");
        offeringTitle = this.addButton(new TextButton(this, this.guiLeft + OFFERING_X, this.guiTop + OFFERING_Y - 10, OFFERING_WIDTH * 2, 12, text, tooltip));
        text = new TranslationTextComponent("gui.favor.trades").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.trades.tooltip");
        tradeTitle = this.addButton(new TextButton(this, this.guiLeft + TRADE_X, this.guiTop + TRADE_Y - 10, TRADE_WIDTH, 12, text, tooltip));
        for(int i = 0; i < OFFERING_COUNT; i++) {
            offeringButtons[i] = this.addButton(new OfferingButton(this, i, this.guiLeft + OFFERING_X + OFFERING_WIDTH * (i % 2), this.guiTop + OFFERING_Y + OFFERING_HEIGHT * (i / 2)));
        }
        for(int i = 0; i < TRADE_COUNT; i++) {
            tradeButtons[i] = this.addButton(new TradeButton(this, i, this.guiLeft + TRADE_X, this.guiTop + TRADE_Y + i * OFFERING_HEIGHT));
        }
        // add sacrifice UI elements
        text = new TranslationTextComponent("gui.favor.sacrifices").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.sacrifices.tooltip");
        sacrificeTitle = this.addButton(new TextButton(this, this.guiLeft + SACRIFICE_X, this.guiTop + SACRIFICE_Y - 12, SACRIFICE_WIDTH, 12, text, tooltip));
        for(int i = 0; i < SACRIFICE_COUNT; i++) {
            sacrificeButtons[i] = this.addButton(new SacrificeButton(this, i, this.guiLeft + SACRIFICE_X, this.guiTop + SACRIFICE_Y + i * SACRIFICE_HEIGHT));
        }
        // add perk UI elements
        for(Map.Entry<ResourceLocation, Map<Integer, List<Perk>>> entry : perkMap.entrySet()) {
            startX = this.guiLeft + PERK_BOUNDS_X;
            startY = this.guiTop + PERK_BOUNDS_Y;
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
                text = new StringTextComponent(perksAtLevel.getKey().toString()).mergeStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
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
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) { }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        // draw background image
        this.getMinecraft().getTextureManager().bindTexture(SCREEN_TEXTURE);
        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        // draw favor boundary
        if(this.page == Page.PERKS) {
            renderFavorLevel(matrixStack, mouseX, mouseY, partialTicks);
        }
        // draw name
        this.font.drawText(matrixStack, deityName, this.guiLeft + NAME_X, this.guiTop + NAME_Y, 0xFFFFFF);
        // draw favor
        this.font.drawText(matrixStack, deityFavor, this.guiLeft + FAVOR_X, this.guiTop + FAVOR_Y, 0xFFFFFF);
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
        if(this.page == Page.PERKS && mouseX > guiLeft + PERK_BOUNDS_X && mouseX < guiLeft + PERK_BOUNDS_X + PERK_BOUNDS_WIDTH
                && mouseY > guiTop + PERK_BOUNDS_Y && mouseY < guiTop + SCREEN_HEIGHT) {
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
    public void onClose() {
        super.onClose();
        // clear entity map
        for(AltarEntity entity : entityMap.values()) {
            entity.remove();
        }
    }

    private void renderSummaryPage(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // draw title
        int startX = (this.width - this.font.getStringPropertyWidth(deityTitle)) / 2;
        this.font.drawText(matrixStack, deityTitle, startX, this.guiTop + TITLE_Y, 0xFFFFFF);
        // draw preview pane
        this.minecraft.getTextureManager().bindTexture(SCREEN_WIDGETS);
        this.blit(matrixStack, this.guiLeft + PREVIEW_X, this.guiTop + PREVIEW_Y, 202, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        // draw favor amounts
        final FavorLevel level = getContainer().getFavor().getFavor(deity);
        this.font.drawText(matrixStack, new TranslationTextComponent("favor.favor").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y, 0xFFFFFF);
        final long curFavor = level.getFavor();
        final long nextFavor = level.getFavorToNextLevel();
        this.font.drawText(matrixStack, new StringTextComponent(curFavor + " / " + nextFavor)
                        .mergeStyle(TextFormatting.DARK_PURPLE),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y + font.FONT_HEIGHT * 1 + 1, 0xFFFFFF);
        this.font.drawText(matrixStack, new TranslationTextComponent("favor.level").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y + font.FONT_HEIGHT * 5 / 2, 0xFFFFFF);
        this.font.drawText(matrixStack, new StringTextComponent(String.valueOf(level.getLevel() + " / " + (curFavor < 0 ? level.getMin() : level.getMax()))).mergeStyle(TextFormatting.DARK_PURPLE),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y + font.FONT_HEIGHT * 7 / 2 + 1, 0xFFFFFF);
        this.font.drawText(matrixStack, new TranslationTextComponent("favor.next_level").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y + font.FONT_HEIGHT * 5, 0xFFFFFF);
        final boolean capped = level.getLevel() == level.getMin() || level.getLevel() == level.getMax();
        this.font.drawText(matrixStack, new StringTextComponent(capped ? "--" : String.valueOf(nextFavor - curFavor)).mergeStyle(TextFormatting.DARK_PURPLE),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y + font.FONT_HEIGHT * 6 + 1, 0xFFFFFF);
        // draw entity
        drawEntityOnScreen(getOrCreateEntity(deity), matrixStack, this.guiLeft + PREVIEW_X + 16, this.guiTop + PREVIEW_Y + 6, (float) mouseX, (float) mouseY, partialTicks);
    }

    private void renderOfferingsPage(MatrixStack matrixStack) {
        // draw scroll background
        this.minecraft.getTextureManager().bindTexture(SCREEN_WIDGETS);
        this.blit(matrixStack, this.guiLeft + SCROLL_X, this.guiTop + SCROLL_Y, 188, 0, SCROLL_WIDTH, SCROLL_HEIGHT);
    }

    private void renderSacrificesPage(MatrixStack matrixStack) {
        // draw scroll background
        this.minecraft.getTextureManager().bindTexture(SCREEN_WIDGETS);
        this.blit(matrixStack, this.guiLeft + SCROLL_X, this.guiTop + SCROLL_Y, 188, 0, SCROLL_WIDTH, SCROLL_HEIGHT);
    }

    private void renderPerksPage(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack.push();
        matrixStack.translate(0, 0, 250);
        // draw header frame
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bindTexture(SCREEN_TEXTURE);
        this.blit(matrixStack, this.guiLeft + 23, this.guiTop + 13, 23, 13, 208, 28);
        // re-draw title and favor
        this.font.drawText(matrixStack, deityName, this.guiLeft + NAME_X, this.guiTop + NAME_Y, 0xFFFFFF);
        this.font.drawText(matrixStack, deityFavor, this.guiLeft + FAVOR_X, this.guiTop + FAVOR_Y, 0xFFFFFF);
        // re-draw level buttons
        for(TextButton b : perkLevelButtonMap.values()) {
            b.renderWidget(matrixStack, mouseX, mouseY, partialTicks);
        }
        // draw side frames
        this.getMinecraft().getTextureManager().bindTexture(SCREEN_TEXTURE);
        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, 24, 170);
        this.blit(matrixStack, this.guiLeft + 232, this.guiTop, 232, 0, 24, 168);
        // draw perk button tooltip
        matrixStack.translate(0, 0, 50);
        for(PerkButton b : perkButtonMap.computeIfAbsent(deity, key -> ImmutableList.of())) {
            if(b.visible && b.isHovered()) {
                b.renderPerkTooltip(matrixStack, mouseX, mouseY);
            }
        }
        matrixStack.pop();
    }

    private void renderFavorLevel(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // draw favor boundary
        int level = getContainer().getFavor().getFavor(deity).getLevel();
        matrixStack.push();
        float sizeX = Math.min(PERK_BOUNDS_WIDTH, (level + 1) * (PERK_WIDTH + PERK_SPACE_X) + this.dx - 9);
        if(sizeX > 0) {
            float scaleX = sizeX / 8.0F;
            float scaleY = PERK_BOUNDS_HEIGHT / 8.0F;
            matrixStack.scale(scaleX, scaleY, 1);
            matrixStack.translate((this.guiLeft + PERK_BOUNDS_X) / scaleX, (this.guiTop + PERK_BOUNDS_Y) / scaleY, 0);
            RenderSystem.enableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.5F);
            this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
            // draw stretched texture
            this.blit(matrixStack, 0, 0, 0, 220, 8, 8);
            // draw boundary texture
            matrixStack.scale(1 / scaleX, 1, 1);
            this.blit(matrixStack, Math.round(sizeX), 0, 8, 220, 8, 8);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        matrixStack.pop();
    }

    /**
     * This method is called whenever a tab or page is changed
     * @param deity the current deity
     */
    public void updateDeity(final ResourceLocation deity) {
        this.deity = deity;
        // update deity name and favor text for header
        deityName = new TranslationTextComponent(Altar.createTranslationKey(deity))
                .mergeStyle(TextFormatting.BLACK);
        final FavorLevel favorLevel = getContainer().getFavor().getFavor(deity);
        deityFavor = new StringTextComponent(favorLevel.getLevel() + " / " + favorLevel.getMax())
                .mergeStyle(TextFormatting.DARK_PURPLE);
        // update based on current page
        int scrollIndex;
        scrollVisible = false;
        scrollEnabled = false;
        switch (this.page) {
            case SUMMARY:
                deityTitle = new TranslationTextComponent(Altar.createTranslationKey(deity) + ".title")
                        .mergeStyle(TextFormatting.BLACK, TextFormatting.ITALIC);
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

    public void updateScroll(final float amount) {
        int scrollIndex;
        switch (this.page) {
            case OFFERINGS:
                scrollIndex = (int) Math.floor(amount * Math.max(offeringCount / 2, tradeCount));
                for(int i = 0; i < OFFERING_COUNT; i++) {
                    offeringButtons[i].updateOffering(deity, scrollIndex);
                }
                for(int i = 0; i < TRADE_COUNT; i++) {
                    tradeButtons[i].updateOffering(deity, scrollIndex);
                }
                break;
            case SACRIFICES:
                scrollIndex = (int) Math.floor(amount * sacrificeCount);
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
        FavorLevel level = getContainer().getFavor().getFavor(deity);
        int minX = -(level.getMax() - 5) * (PERK_WIDTH + PERK_SPACE_X);
        int maxX = -(level.getMin() - 2) * (PERK_WIDTH + PERK_SPACE_Y);
        int minY = 0;
        int maxY = 0;
        for(List<Perk> l : perkMap.getOrDefault(deity, ImmutableMap.of()).values()) {
            if(l.size() > maxY) {
                maxY = l.size();
            }
        }
        maxY = Math.max(0, maxY - 4) * (PERK_HEIGHT + PERK_SPACE_Y);
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
            Altar altar = RPGGods.ALTAR.get(deity).orElse(Altar.EMPTY);
            AltarEntity altarEntity = AltarEntity.createAltar(playerInventory.player.world, BlockPos.ZERO, Direction.NORTH, altar);
            altarEntity.setNoGravity(true);
            altarEntity.noClip = true;
            entityMap.put(deity, altarEntity);
        }
        return entityMap.get(deity);
    }

    @SuppressWarnings("deprecation")
    public void drawEntityOnScreen(final AltarEntity entity, final MatrixStack matrixStackIn, final int posX, final int posY,
                                   final float mouseX, final float mouseY, final float partialTicks) {
        float margin = 12;
        float scale = PREVIEW_WIDTH - margin * 2;
        float rotX = (float) Math.atan((double) ((mouseX - this.guiLeft) / 40.0F));
        float rotY = (float) Math.atan((double) ((mouseY - this.guiTop - PREVIEW_HEIGHT / 2) / 40.0F));

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

        RenderHelper.setupGuiFlatDiffuseLighting();

        IRenderTypeBuffer.Impl bufferType = minecraft.getRenderTypeBuffers().getBufferSource();
        Minecraft.getInstance().getRenderManager().getRenderer(entity)
                .render(entity, 0F, partialTicks, matrixStackIn, bufferType, 15728880);
        bufferType.finish();

        RenderSystem.enableDepthTest();
        RenderHelper.setupGui3DDiffuseLighting();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
        RenderSystem.popMatrix();
    }

    protected class PerkButton extends Button {

        private Perk perk;
        private List<ITextComponent> perkTypes;
        private List<ITextComponent> perkConditions;
        private ITextComponent perkChance;
        private ITextComponent perkRange;
        private boolean enabled;
        private int tooltipWidth;

        public PerkButton(final FavorScreen gui, final Perk perk, int x, int y) {
            super(x, y, PERK_WIDTH, PERK_HEIGHT, StringTextComponent.EMPTY, b -> {});
            this.perkTypes = new ArrayList<>();
            this.perkConditions = new ArrayList<>();
            this.setPerk(perk);
            setEnabled(true);
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && this.enabled && perk != null) {
                // draw color
                RenderSystem.color4f(perk.getIcon().getColorRed(), perk.getIcon().getColorGreen(), perk.getIcon().getColorBlue(), 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, 0, 196, PERK_WIDTH, PERK_HEIGHT);
                // draw bg
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.blit(matrixStack, this.x, this.y, 22, 196, PERK_WIDTH, PERK_HEIGHT);
                // draw item
                FavorScreen.this.itemRenderer.renderItemIntoGUI(perk.getIcon().getItem(), this.x + (PERK_WIDTH - 16) / 2, this.y + (PERK_HEIGHT - 16) / 2);
                // draw cooldown
                long timeElapsed = openTimestamp - playerInventory.player.world.getGameTime();
                long cooldown = getContainer().getFavor().getPerkCooldown(this.perk.getCategory()) - timeElapsed;
                // render cooldown texture on top of item
                if(cooldown > 0 && perk.getCooldown() > 0) {
                    matrixStack.push();
                    matrixStack.translate(0, 0, FavorScreen.this.itemRenderer.zLevel + 110);
                    // determine v offset
                    int vOffset = Math.round((1.0F - MathHelper.clamp((float)cooldown / (float)perk.getCooldown(), 0.0F, 1.0F)) * PERK_HEIGHT);
                    // draw cooldown
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.5F);
                    FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                    this.blit(matrixStack, this.x, this.y + vOffset, 44, 196 + vOffset, PERK_WIDTH, PERK_HEIGHT - vOffset);
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                    matrixStack.pop();
                }
            }
        }

        public void setPerk(final Perk perk) {
            this.perk = perk;
            this.perkTypes.clear();
            this.perkConditions.clear();
            this.perkChance = StringTextComponent.EMPTY;
            this.perkRange = StringTextComponent.EMPTY;
            if(perk != null) {
                // add all perk action titles
                for(PerkData data : perk.getActions()) {
                    perkTypes.add(data.getDisplayName().mergeStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE));
                    perkTypes.add(data.getDisplayDescription().mergeStyle(TextFormatting.BLUE));
                }
                // add perk condition texts
                for(PerkCondition condition : perk.getConditions()) {
                    // do not show "random tick" conditions
                    if(condition.getType() != PerkCondition.Type.RANDOM_TICK) {
                        perkConditions.add(condition.getDisplayName().mergeStyle(TextFormatting.DARK_GRAY));
                    }
                }
                // add prefix to each condition based on plurality
                if (perkConditions.size() > 0) {
                    // add prefix to first condition
                    ITextComponent t2 = new TranslationTextComponent("favor.perk.condition.single", perkConditions.get(0))
                            .mergeStyle(TextFormatting.DARK_GRAY);
                    perkConditions.set(0, t2);
                    // add prefix to following conditions
                    for (int i = 1, l = perkConditions.size(); i < l; i++) {
                        t2 = new TranslationTextComponent("favor.perk.condition.multiple", perkConditions.get(i))
                                .mergeStyle(TextFormatting.DARK_GRAY);
                        perkConditions.set(i, t2);
                    }
                }
                // add text to display favor range
                FavorLevel favorLevel = FavorScreen.this.getContainer().getFavor().getFavor(perk.getDeity());
                TextFormatting color = perk.getRange().isInRange(favorLevel.getLevel()) ? TextFormatting.DARK_GREEN : TextFormatting.RED;
                if(perk.getRange().getMaxLevel() == favorLevel.getMax()) {
                    perkRange = new TranslationTextComponent("gui.favor.perk.range.above_min", perk.getRange().getMinLevel()).mergeStyle(color);
                } else if(perk.getRange().getMinLevel() == favorLevel.getMin()) {
                    perkRange = new TranslationTextComponent("gui.favor.perk.range.below_max", perk.getRange().getMaxLevel()).mergeStyle(color);
                } else {
                    perkRange = new TranslationTextComponent("gui.favor.perk.range.between",
                            perk.getRange().getMinLevel(), perk.getRange().getMaxLevel()).mergeStyle(color);
                }
                // add perk chance (formatted as 2 or fewer decimals)
                String chanceString = String.format("%.2f", perk.getChance() * 100.0F).replaceAll("0*$", "").replaceAll("\\.$", "");
                perkChance = new TranslationTextComponent("gui.favor.perk.chance", chanceString)
                        .mergeStyle(TextFormatting.BLACK);
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
                    !( this.x > FavorScreen.this.guiLeft + PERK_BOUNDS_X + PERK_BOUNDS_WIDTH
                    || this.x < FavorScreen.this.guiLeft + PERK_BOUNDS_X - this.width
                    || this.y > FavorScreen.this.guiTop + PERK_BOUNDS_Y + PERK_BOUNDS_HEIGHT
                    || this.y < FavorScreen.this.guiTop + PERK_BOUNDS_Y - this.height);
        }

        public void renderPerkTooltip(MatrixStack matrixStack, final int mouseX, final int mouseY) {
            int startX = mouseX + 10;
            int startY = mouseY - PERK_TOOLTIP_HEIGHT / 2;
            final int lineHeight = 11;
            // determine background size
            final int lines = perkTypes.size() + perkConditions.size() + 6;
            renderPerkTooltipBackground(matrixStack, startX, startY, tooltipWidth + 14, lines * lineHeight + 6);
            startX += 6;
            startY += 14;
            // draw perk data
            int line = 0;
            // draw title(s)
            for (ITextComponent t : perkTypes) {
                FavorScreen.this.font.drawText(matrixStack, t, startX, startY + lineHeight * (line++), 0xFFFFFF);
            }
            // line space
            startY += 5;
            // draw conditions
            for (ITextComponent t : perkConditions) {
                FavorScreen.this.font.drawText(matrixStack, t, startX, startY + lineHeight * (line++), 0xFFFFFF);
            }
            if(!perkConditions.isEmpty()) {
                startY += 5;
            }
            // draw chance
            FavorScreen.this.font.drawText(matrixStack, perkChance, startX, startY + lineHeight * (line++), 0xFFFFFF);
            // line space
            startY += 5;
            // draw range
            ITextComponent unlock = new TranslationTextComponent("gui.favor.perk.unlock")
                    .mergeStyle(TextFormatting.BLACK);
            FavorScreen.this.font.drawText(matrixStack, unlock, startX, startY + lineHeight * (line++), 0xFFFFFF);
            FavorScreen.this.font.drawText(matrixStack, perkRange, startX, startY + lineHeight * (line++), 0xFFFFFF);
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
            matrixStack.push();
            matrixStack.translate(startX, startY, 0);
            // prepare to render
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
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
            matrixStack.pop();
        }

        /**
         * Iterates through all text components attached to this button's tooltip
         * @return the maximum width of this button's text components
         */
        private int calculateWidth() {
            int maxWidth = 0;
            for(ITextComponent t : perkTypes) {
                maxWidth = Math.max(maxWidth, FavorScreen.this.font.getStringPropertyWidth(t));
            }
            for(ITextComponent t : perkConditions) {
                maxWidth = Math.max(maxWidth, FavorScreen.this.font.getStringPropertyWidth(t));
            }
            maxWidth = Math.max(maxWidth, FavorScreen.this.font.getStringPropertyWidth(perkChance));
            maxWidth = Math.max(maxWidth, FavorScreen.this.font.getStringPropertyWidth(perkRange));
            return maxWidth;
        }
    }

    protected class TradeButton extends OfferingButton {
        protected static final int ARROW_WIDTH = 12;
        protected static final int ARROW_HEIGHT = 9;
        protected ITextComponent tradeTooltip;
        protected ITextComponent unlockText;
        protected ITextComponent unlockTooltip;

        public TradeButton(FavorScreen gui, int index, int x, int y) {
            super(gui, index, x, y, TRADE_WIDTH, OFFERING_HEIGHT);
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && offering != null) {
                // draw item
                FavorScreen.this.itemRenderer.renderItemIntoGUI(offering.getAccept(), this.x, this.y);
                // draw trade
                FavorScreen.this.itemRenderer.renderItemIntoGUI(offering.getTrade().get(), this.x + 18 + ARROW_WIDTH, this.y);
                // draw favor text
                FavorScreen.this.font.drawText(matrixStack, unlockText, this.x + 18 * 2 + ARROW_WIDTH + 4, this.y + textY, 0xFFFFFF);
                // draw function text
                if(offering != null && offering.getFunction().isPresent()) {
                    FavorScreen.this.font.drawText(matrixStack, functionText, this.x + 18 * 3 + ARROW_WIDTH - 4, this.y + textY, 0xFFFFFF);
                }
                // draw arrow
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x + 18, this.y + textY, 113, 130, ARROW_WIDTH, ARROW_HEIGHT);
            }
        }

        @Override
        public void updateOffering(final ResourceLocation deity, final int startIndex) {
            final int offeringId = startIndex * 2 + id;
            final List<Offering> offerings = FavorScreen.this.tradeMap.getOrDefault(deity, ImmutableList.of());
            if(offeringId < offerings.size()) {
                this.visible = true;
                updateOffering(offerings.get(offeringId));
            } else {
                this.visible = false;
            }
        }

        @Override
        protected void updateOffering(final Offering offering) {
            super.updateOffering(offering);
            // determine item tooltip
            if(offering.getTrade().isPresent()) {
                this.tradeTooltip = offering.getTrade().get().getDisplayName();
                this.unlockText = new StringTextComponent("" + offering.getTradeMinLevel()).mergeStyle(TextFormatting.DARK_PURPLE);
                this.unlockTooltip = new TranslationTextComponent("gui.favor.offering.unlock.tooltip", offering.getTradeMinLevel());
            }
        }

        @Override
        protected Optional<ITextComponent> getTooltip(final int mouseX, final int mouseY) {
            if(offering != null) {
                if(offering.getFunction().isPresent() && mouseX >= (this.x + 18 * 3 + ARROW_WIDTH - 4)) {
                    return Optional.of(functionTooltip);
                }
                if(mouseX >= (this.x + 18 * 2 + ARROW_WIDTH) && mouseX <= (this.x + 18 * 3 + ARROW_WIDTH - 4)) {
                    return Optional.of(unlockTooltip);
                }
                if(offering.getTrade().isPresent() && mouseX >= (this.x + 18 + ARROW_WIDTH) && mouseX <= (this.x + 18 * 2 + ARROW_WIDTH)) {
                    return Optional.of(tradeTooltip);
                }
                if(mouseX <= (this.x + 18)) {
                    return Optional.of(itemTooltip);
                }
            }
            return Optional.empty();
        }
    }

    protected class OfferingButton extends Button {
        protected final int textY = 5;
        protected int id;
        protected Offering offering;
        protected ITextComponent itemTooltip;
        protected ITextComponent favorText;
        protected final ITextComponent functionText;
        protected final ITextComponent functionTooltip;

        public OfferingButton(final FavorScreen gui, final int index, final int x, final int y) {
            this(gui, index, x, y, OFFERING_WIDTH, OFFERING_HEIGHT);
        }

        public OfferingButton(final FavorScreen gui, final int index, int x, int y,final int width, final int height) {
            super(x, y, width, height, StringTextComponent.EMPTY, b -> {},
                    (b, m, bx, by) -> ((OfferingButton)b).getTooltip(bx, by).ifPresent(t -> gui.renderTooltip(m, t, bx, by)));
            this.id = index;
            this.itemTooltip = StringTextComponent.EMPTY;
            this.favorText = StringTextComponent.EMPTY;
            this.functionText = new StringTextComponent(" \u2605 ").mergeStyle(TextFormatting.BLUE);
            this.functionTooltip = new TranslationTextComponent("gui.favor.offering.function.tooltip");
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && offering != null) {
                // draw item
                FavorScreen.this.itemRenderer.renderItemIntoGUI(offering.getAccept(), this.x, this.y);
                // draw favor text
                FavorScreen.this.font.drawText(matrixStack, favorText, this.x + 18, this.y + textY, 0xFFFFFF);
                // draw function text
                if(offering != null && offering.getFunction().isPresent()) {
                    FavorScreen.this.font.drawText(matrixStack, functionText, this.x + 18 * 2 - 2, this.y + textY, 0xFFFFFF);
                }
            }
        }

        public void updateOffering(final ResourceLocation deity, final int startIndex) {
            final int offeringId = startIndex * 2 + id;
            final List<Offering> offerings = FavorScreen.this.offeringMap.getOrDefault(deity, ImmutableList.of());
            if(offeringId < offerings.size()) {
                this.visible = true;
                updateOffering(offerings.get(offeringId));
            } else {
                this.visible = false;
            }
        }

        protected void updateOffering(final Offering offering) {
            this.offering = offering;
            // determine item tooltip
            this.itemTooltip = offering.getAccept().getDisplayName();
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
            this.favorText = new StringTextComponent(favorString).mergeStyle(color);
        }

        protected Optional<ITextComponent> getTooltip(final int mouseX, final int mouseY) {
            if(offering != null && offering.getFunction().isPresent() && mouseX >= (this.x + 18 * 2 - 2)) {
                return Optional.of(functionTooltip);
            }
            if(mouseX <= (this.x + 18)) {
                return Optional.of(itemTooltip);
            }
            return Optional.empty();
        }
    }

    protected class SacrificeButton extends Button {

        private int id;
        private Sacrifice sacrifice;
        protected ITextComponent entityText;
        protected ITextComponent favorText;
        protected final ITextComponent functionText;
        protected final ITextComponent functionTooltip;

        public SacrificeButton(final FavorScreen gui, final int index, int x, int y) {
            super(x, y, SACRIFICE_WIDTH, SACRIFICE_HEIGHT, StringTextComponent.EMPTY, b -> {},
                    (b, m, bx, by) -> ((SacrificeButton)b).getTooltip(bx, by).ifPresent(t -> gui.renderTooltip(m, t, bx, by)));
            this.id = index;
            this.entityText = StringTextComponent.EMPTY;
            this.favorText = StringTextComponent.EMPTY;
            this.functionText = new StringTextComponent(" \u2605 ").mergeStyle(TextFormatting.BLUE);
            this.functionTooltip = new TranslationTextComponent("gui.favor.sacrifice.function.tooltip");
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && sacrifice != null) {
                // draw entity text
                FavorScreen.this.font.drawText(matrixStack, entityText, this.x, this.y, 0xFFFFFF);
                // draw favor text
                FavorScreen.this.font.drawText(matrixStack, favorText, this.x + 18 * 7, this.y, 0xFFFFFF);
                // draw function text
                if(sacrifice != null && sacrifice.getFunction().isPresent()) {
                    FavorScreen.this.font.drawText(matrixStack, functionText, this.x + 18 * 8, this.y, 0xFFFFFF);
                }
            }
        }

        public void updateSacrifice(final ResourceLocation deity, final int startIndex) {
            final int sacrificeId = startIndex * 2 + id;
            final List<Sacrifice> sacrifices = FavorScreen.this.sacrificeMap.getOrDefault(deity, ImmutableList.of());
            if(sacrificeId < sacrifices.size()) {
                this.visible = true;
                updateSacrifice(sacrifices.get(sacrificeId));
            } else {
                this.visible = false;
            }
        }

        protected void updateSacrifice(final Sacrifice sacrifice) {
            this.sacrifice = sacrifice;
            // determine entity text
            EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(sacrifice.getEntity());
            if(entityType != null) {
                this.entityText = new TranslationTextComponent(entityType.getTranslationKey()).mergeStyle(TextFormatting.BLACK);
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
            this.favorText = new StringTextComponent(favorString).mergeStyle(color);
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
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && this.enabled) {
                // draw text
                FavorScreen.this.font.drawText(matrixStack, getMessage(), this.x, this.y, 0xFFFFFF);
            }
        }

        public void move(final int moveX) {
            this.x += moveX;
            this.visible = this.enabled &&
                    !(this.x > FavorScreen.this.guiLeft + PERK_BOUNDS_X + PERK_BOUNDS_WIDTH
                    || this.x < FavorScreen.this.guiLeft + PERK_BOUNDS_X - this.width);
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }
    }

    protected class TabButton extends Button {

        private int id;
        private ItemStack item = ItemStack.EMPTY;

        public TabButton(final FavorScreen gui, final int index, final ITextComponent title, final int x, final int y) {
            super(x, y, TAB_WIDTH, TAB_HEIGHT, title, b -> gui.updateTab(index),
                    (b, m, bx, by) -> gui.renderTooltip(m, b.getMessage(), bx, by));
            this.id = index;
            updateDeity();
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final boolean selected = FavorScreen.this.tab == id;
                int dY = selected ? 0 : 4;
                final int u = (id % TAB_COUNT) * TAB_WIDTH;
                final int v = selected ? this.height : dY;
                // draw button background
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, u, v, this.width, this.height - dY);
                // draw item
                FavorScreen.this.itemRenderer.renderItemIntoGUI(item, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
            }
        }

        public void updateDeity() {
            final int deityId = id + (FavorScreen.this.tabGroup * FavorScreen.this.tabCount);
            if(deityId < FavorScreen.this.deityList.size()) {
                this.visible = true;
                final ResourceLocation deity = FavorScreen.this.deityList.get(deityId);
                this.setMessage(new TranslationTextComponent(Altar.createTranslationKey(deity)));
                item = RPGGods.ALTAR.get(deity).orElse(Altar.EMPTY).getIcon();
            } else {
                this.visible = false;
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
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final int u = left ? ARROW_WIDTH : 0;
                final int v = 130 + (isHovered() ? this.height : 0);
                // draw button
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
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
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final boolean selected = FavorScreen.this.page == this.page;
                int dY = selected ? 0 : -4;
                int uX = (page.ordinal() % PAGE_COUNT) * PAGE_WIDTH;
                int vY = 64 + (selected ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
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
