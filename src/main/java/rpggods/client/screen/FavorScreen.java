package rpggods.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.favor.FavorLevel;
import rpggods.favor.IFavor;
import rpggods.gui.AltarContainer;
import rpggods.gui.FavorContainer;

import java.util.ArrayList;
import java.util.List;


public class FavorScreen extends ContainerScreen<FavorContainer> {

    // CONSTANTS
    private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/gui/favor.png");
    private static final ResourceLocation SCREEN_WIDGETS = new ResourceLocation(RPGGods.MODID, "textures/gui/favor_widgets.png");
    private static final ResourceLocation PERK = new ResourceLocation(RPGGods.MODID, "textures/gui/perk.png");

    private static final int SCREEN_WIDTH = 224;
    private static final int SCREEN_HEIGHT = 162;

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_COUNT = 7;

    private static final int PAGE_WIDTH = 56;
    private static final int PAGE_HEIGHT = 32;
    private static final int PAGE_COUNT = 4;

    private static final int ARROW_WIDTH = 14;
    private static final int ARROW_HEIGHT = 18;

    // Header
    private static final int NAME_X = 5;
    private static final int NAME_Y = 6;
    private static final int FAVOR_X = 175;
    private static final int FAVOR_Y = NAME_Y;

    // Summary page
    private static final int TITLE_X = 7;
    private static final int TITLE_Y = 130;
    private static final int PREVIEW_X = 7;
    private static final int PREVIEW_Y = 36;
    private static final int PREVIEW_WIDTH = 54;
    private static final int PREVIEW_HEIGHT = 80;
    private static final int SUMMARY_X = PREVIEW_X + PREVIEW_WIDTH + 32;
    private static final int SUMMARY_Y = PREVIEW_Y;

    // Offerings page

    // Sacrifices page

    // Perks page

    private final FavorScreen.TabButton[] tabButtons = new FavorScreen.TabButton[TAB_COUNT];
    private final FavorScreen.PageButton[] pageButtons = new FavorScreen.PageButton[PAGE_COUNT];

    private final List<ResourceLocation> deityList = new ArrayList<>();
    private ResourceLocation deity;
    private IFormattableTextComponent deityName = (IFormattableTextComponent) StringTextComponent.EMPTY;
    private IFormattableTextComponent deityFavor = (IFormattableTextComponent) StringTextComponent.EMPTY;
    private int tabGroupCount;
    private int tabGroup;
    private int tabCount;
    private int tab;
    private Page page = Page.SUMMARY;

    // summary page
    private IFormattableTextComponent deityTitle = (IFormattableTextComponent) StringTextComponent.EMPTY;

    public FavorScreen(FavorContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = SCREEN_WIDTH;
        this.ySize = SCREEN_HEIGHT;
        // add all deities to list
        final IFavor favor = screenContainer.getFavor();
        deityList.clear();
        deityList.addAll(RPGGods.ALTAR.getKeys());
        deityList.sort((d1, d2) -> favor.getFavor(d2).compareToAbs(favor.getFavor(d1)));
        if(deityList.size() > 0) {
            // determine current page
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
        for(int i = 0; i < tabCount; i++) {
            tabButtons[i] = this.addButton(new TabButton(this, i, new TranslationTextComponent(Altar.createTranslationKey(deityList.get(i))),
                    guiLeft + (i * TAB_WIDTH), guiTop - TAB_HEIGHT + 4));
        }
        // add pages
        for(int i = 0; i < PAGE_COUNT; i++) {
            FavorScreen.Page p = FavorScreen.Page.values()[i];
            pageButtons[i] = this.addButton(new PageButton(this, guiLeft + (i * PAGE_WIDTH), guiTop + SCREEN_HEIGHT - 4,
                    new TranslationTextComponent(p.getTooltip()), p));
        }
        // update
        updateTabGroup(0);
        updateTab(0);
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
        // draw title
        this.font.drawText(matrixStack, deityName, this.guiLeft + NAME_X, this.guiTop + NAME_Y, 0xFFFFFF);
        // draw favor
        this.font.drawText(matrixStack, deityFavor, this.guiLeft + FAVOR_X, this.guiTop + FAVOR_Y, 0xFFFFFF);
        // render page-specific items
        switch (this.page) {
            case SUMMARY:
                renderSummaryPage(matrixStack);
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

    private void renderSummaryPage(MatrixStack matrixStack) {
        // draw title
        int titleX = (this.xSize - this.font.getStringWidth(deityTitle.getUnformattedComponentText())) / 2;
        this.font.drawText(matrixStack, deityTitle, this.guiLeft + titleX, this.guiTop + TITLE_Y, 0xFFFFFF);
        // draw preview pane
        this.minecraft.getTextureManager().bindTexture(SCREEN_WIDGETS);
        this.blit(matrixStack, this.guiLeft + PREVIEW_X, this.guiTop + PREVIEW_Y, 168, 130, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        // draw favor amounts
        final FavorLevel level = getContainer().getFavor().getFavor(deity);
        this.font.drawText(matrixStack, new TranslationTextComponent("favor.favor").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC),
                guiLeft + SUMMARY_X, guiTop + SUMMARY_Y, 0xFFFFFF);
        final long curFavor = level.getFavor();
        final long nextFavor = level.getFavorToNextLevel();
        this.font.drawText(matrixStack, new StringTextComponent(String.valueOf(curFavor + " / " + nextFavor))
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
    }

    private void renderOfferingsPage(MatrixStack matrixStack) {

    }

    private void renderSacrificesPage(MatrixStack matrixStack) {

    }

    private void renderPerksPage(MatrixStack matrixStack) {

    }

    public void updateDeity(final ResourceLocation deity) {
        this.deity = deity;
        // update deity name and favor text
        deityName = new TranslationTextComponent(Altar.createTranslationKey(deity))
                .mergeStyle(TextFormatting.BLACK);
        deityTitle = new TranslationTextComponent(Altar.createTranslationKey(deity) + ".title")
                .mergeStyle(TextFormatting.BLACK, TextFormatting.ITALIC);
        final FavorLevel favorLevel = getContainer().getFavor().getFavor(deity);
        deityFavor = new StringTextComponent(favorLevel.getLevel() + " / " + favorLevel.getMax())
                .mergeStyle(TextFormatting.DARK_PURPLE);
        // TODO: other stuff
    }

    public void updateTabGroup(final int tabGroup) {
        this.tabGroup = tabGroup % tabGroupCount;
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
        boolean page0 = page == Page.SUMMARY;
        boolean page1 = page == Page.OFFERINGS;
        boolean page2 = page == Page.SACRIFICES;
        boolean page3 = page == Page.PERKS;
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
                int dY = selected ? 0 : 2;
                final int u = (id % TAB_COUNT) * TAB_WIDTH;
                final int v = selected ? this.height : 2;
                // draw button background
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y - dY, u, v - dY, this.width, this.height - dY);
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
                final ResourceLocation altar = new ResourceLocation(deity.getNamespace(), "altar_" + deity.getPath());
                item = ForgeRegistries.ITEMS.containsKey(altar) ? new ItemStack(ForgeRegistries.ITEMS.getValue(altar)) : ItemStack.EMPTY;
            } else {
                this.visible = false;
            }
        }
    }

    protected class PageButton extends Button {

        private final Page page;

        public PageButton(final FavorScreen screenIn, final int x, final int y, final ITextComponent title, final Page page) {
            super(x, y, PAGE_WIDTH, PAGE_HEIGHT, title, b -> screenIn.updatePage(page),
                    (b, m, bx, by) -> screenIn.renderTooltip(m, title, bx, by));
            this.page = page;
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                final boolean selected = FavorScreen.this.page == this.page;
                int dY = selected ? 0 : -2;
                final int u = 0;
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

        private String tooltip;

        private Page(final String tooltipIn) {
            tooltip = tooltipIn;
        }

        public String getTooltip() { return tooltip; }
    }
}
