package rpggods.client.screen;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.entity.AltarEntity;
import rpggods.favor.FavorLevel;
import rpggods.favor.IFavor;
import rpggods.gui.FavorContainer;
import rpggods.network.CUpdateAltarPacket;
import rpggods.perk.Perk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class FavorScreen extends ContainerScreen<FavorContainer> {

    // CONSTANTS
    private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/gui/favor.png");
    private static final ResourceLocation SCREEN_WIDGETS = new ResourceLocation(RPGGods.MODID, "textures/gui/favor_widgets.png");
    private static final ResourceLocation PERK = new ResourceLocation(RPGGods.MODID, "textures/gui/perk.png");

    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 168;

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_COUNT = 6;

    private static final int PAGE_WIDTH = 47;
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

    // Data
    private static final List<ResourceLocation> deityList = new ArrayList<>();
    private static final Map<ResourceLocation, AltarEntity> entityMap = new HashMap<>();
    private static final Map<ResourceLocation, List<Offering>> offeringMap = new HashMap();
    private static final Map<ResourceLocation, List<Offering>> tradeMap = new HashMap();
    private static final Map<ResourceLocation, List<Sacrifice>> sacrificeMap = new HashMap();
    private static final Map<ResourceLocation, List<Perk>> perkMap = new HashMap();
    private ResourceLocation deity;
    private IFormattableTextComponent deityName = (IFormattableTextComponent) StringTextComponent.EMPTY;
    private IFormattableTextComponent deityFavor = (IFormattableTextComponent) StringTextComponent.EMPTY;
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


    public FavorScreen(FavorContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = SCREEN_WIDTH;
        this.ySize = SCREEN_HEIGHT;
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
        for(Optional<Offering> offering : RPGGods.OFFERING.getValues()) {
            offering.ifPresent(o -> {
                Map<ResourceLocation, List<Offering>> map = o.getTrade().isPresent() ? tradeMap : offeringMap;
                if(!map.containsKey(o.getDeity())) {
                    map.put(o.getDeity(), new ArrayList<>());
                }
                map.get(o.getDeity()).add(o);
            });
        }
        offeringCount = OFFERING_COUNT;
        tradeCount = TRADE_COUNT;
        // add all sacrifices to map
        sacrificeMap.clear();
        for(Optional<Sacrifice> sacrifice : RPGGods.SACRIFICE.getValues()) {
            sacrifice.ifPresent(o -> {
                if(!sacrificeMap.containsKey(o.getDeity())) {
                    sacrificeMap.put(o.getDeity(), new ArrayList<>());
                }
                sacrificeMap.get(o.getDeity()).add(o);
            });
        }
        sacrificeCount = SACRIFICE_COUNT;
        // add all perks to map
        perkMap.clear();
        for(Optional<Perk> perk : RPGGods.PERK.getValues()) {
            perk.ifPresent(o -> {
                if(!perkMap.containsKey(o.getDeity())) {
                    perkMap.put(o.getDeity(), new ArrayList<>());
                }
                perkMap.get(o.getDeity()).add(o);
            });
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
        // add tabs
        int startX = (this.xSize - (TAB_WIDTH * TAB_COUNT)) / 2;
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
                    new TranslationTextComponent(p.getTitle()), new TranslationTextComponent(p.getTitle() + ".tooltip"), p));
        }
        // add scroll bar
        scrollButton = this.addButton(new ScrollButton<>(this, this.guiLeft + SCROLL_X, this.guiTop + SCROLL_Y, SCROLL_WIDTH, SCROLL_HEIGHT,
                0, 165, SCREEN_WIDGETS, true, gui -> gui.scrollEnabled, b -> updateScroll(b.getScrollAmount())));
        // re-usable text components
        ITextComponent text;
        ITextComponent tooltip;
        // add offering UI elements
        text = new TranslationTextComponent("gui.favor.offerings").mergeStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.offerings.tooltip");
        offeringTitle = this.addButton(new TextButton(this, this.guiLeft + OFFERING_X, this.guiTop + OFFERING_Y - 10, OFFERING_WIDTH * 2, 12, text, tooltip));
        text = new TranslationTextComponent("gui.favor.trades").mergeStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.trades.tooltip");
        tradeTitle = this.addButton(new TextButton(this, this.guiLeft + TRADE_X, this.guiTop + TRADE_Y - 10, TRADE_WIDTH, 12, text, tooltip));
        for(int i = 0; i < OFFERING_COUNT; i++) {
            offeringButtons[i] = this.addButton(new OfferingButton(this, i, this.guiLeft + OFFERING_X + OFFERING_WIDTH * (i % 2), this.guiTop + OFFERING_Y + OFFERING_HEIGHT * (i / 2)));
        }
        for(int i = 0; i < TRADE_COUNT; i++) {
            tradeButtons[i] = this.addButton(new TradeButton(this, i, this.guiLeft + TRADE_X, this.guiTop + TRADE_Y + i * OFFERING_HEIGHT));
        }
        // add sacrifice UI elements
        text = new TranslationTextComponent("gui.favor.sacrifices").mergeStyle(TextFormatting.BLACK, TextFormatting.UNDERLINE);
        tooltip = new TranslationTextComponent("gui.favor.sacrifices.tooltip");
        sacrificeTitle = this.addButton(new TextButton(this, this.guiLeft + SACRIFICE_X, this.guiTop + SACRIFICE_Y - 12, SACRIFICE_WIDTH, 12, text, tooltip));
        for(int i = 0; i < SACRIFICE_COUNT; i++) {
            sacrificeButtons[i] = this.addButton(new SacrificeButton(this, i, this.guiLeft + SACRIFICE_X, this.guiTop + SACRIFICE_Y + i * SACRIFICE_HEIGHT));
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
                renderPerksPage(matrixStack);
                break;
        }
        // draw buttons
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw hovering text LAST
        for(final Widget b : this.buttons) {
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
        this.font.drawText(matrixStack, deityTitle, this.guiLeft + TITLE_X, this.guiTop + TITLE_Y, 0xFFFFFF);
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

    private void renderPerksPage(MatrixStack matrixStack) {

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

    protected class TradeButton extends OfferingButton {
        protected static final int ARROW_WIDTH = 10;
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
                this.blit(matrixStack, this.x + 18, this.y + textY, 115, 130, ARROW_WIDTH, ARROW_HEIGHT);
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
            }
            if(offering.getTradeMinLevel().isPresent()) {
                this.unlockText = new StringTextComponent(offering.getTradeMinLevel().get().toString()).mergeStyle(TextFormatting.DARK_PURPLE);
                this.unlockTooltip = new TranslationTextComponent("gui.favor.offering.unlock.tooltip", offering.getTradeMinLevel().get());
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

        public TextButton(final Screen gui, int x, int y, int width, int height, ITextComponent title, ITextComponent tooltip) {
            super(x, y, width, height, title, b -> {},
                    (b, m, bx, by) -> gui.renderTooltip(m, tooltip, bx, by));
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                // draw text
                FavorScreen.this.font.drawText(matrixStack, getMessage(), this.x, this.y, 0xFFFFFF);
            }
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

        public PageButton(final FavorScreen screenIn, final int x, final int y,
                          final ITextComponent title, final ITextComponent tooltip, final Page page) {
            super(x, y, PAGE_WIDTH, PAGE_HEIGHT, title, b -> screenIn.updatePage(page),
                    (b, m, bx, by) -> screenIn.renderTooltip(m, tooltip, bx, by));
            this.page = page;
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final boolean selected = FavorScreen.this.page == this.page;
                int dY = selected ? 0 : -4;
                final int u = (page.ordinal() % PAGE_COUNT) * PAGE_WIDTH;
                final int v = 64 + (selected ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y - dY, u, v, this.width, this.height + dY);
                drawCenteredString(matrixStack, FavorScreen.this.font, this.getMessage(), this.x + this.width / 2, this.y - dY + (this.height - 8) / 2, getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
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
