/*
package rpggods.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import greekfantasy.GFRegistry;
import greekfantasy.GreekFantasy;
import greekfantasy.deity.IDeity;
import greekfantasy.deity.favor.FavorLevel;
import greekfantasy.deity.favor.FavorRange;
import greekfantasy.deity.favor.IFavor;
import greekfantasy.deity.favor_effect.ConfiguredFavorRange;
import greekfantasy.deity.favor_effect.FavorEffect;
import greekfantasy.deity.favor_effect.SpecialFavorEffect;
import greekfantasy.gui.DeityContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class FavorScreen extends ContainerScreen<FavorContainer> {
  
  private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(GreekFantasy.MODID, "textures/gui/favor.png");
  private static final ResourceLocation TABS_TEXTURE = new ResourceLocation(GreekFantasy.MODID, "textures/gui/favor_tabs.png");

  private static final int SCREEN_WIDTH = 196;
  private static final int SCREEN_HEIGHT = 160;
  
  private static final int TAB_WIDTH = 28;
  private static final int TAB_HEIGHT = 32;
  private static final int TAB_COUNT = 7;
  
  private static final int ARROW_WIDTH = 14;
  private static final int ARROW_HEIGHT = 18;
  
  // private static final int FAVOR_WIDTH = 69;
  // private static final int FAVOR_HEIGHT = 86;
  private static final int FAVOR_LEFT = 9;
  private static final int FAVOR_TOP = 16;
  
  private static final int BTN_LEFT = 7;
  private static final int BTN_TOP = 89;
  private static final int BTN_WIDTH = 70;
  private static final int BTN_HEIGHT = 16;

  private static final int ITEM_LEFT = 80;
  private static final int ITEM_TOP = 25;
  private static final int ITEM_WIDTH = 48;
  private static final int ITEM_HEIGHT = 18;
  private static final int ITEM_COUNT_X = 2;
  private static final int ITEM_COUNT_Y = 7;
  
  private static final int ENTITY_LEFT = ITEM_LEFT;
  private static final int ENTITY_TOP = 32;
  private static final int ENTITY_COUNT_Y = 13;
  private static final int ENTITY_WIDTH = 90;
  
  private static final int BLESSING_LEFT = ENTITY_LEFT;
  private static final int BLESSING_TOP = ENTITY_TOP;
  private static final int BLESSING_COUNT_Y = 6;
  private static final int BLESSING_WIDTH = ENTITY_WIDTH;
  
  private static final int HOSTILE_LEFT = ENTITY_LEFT + 9;
  private static final int HOSTILE_TOP = ENTITY_TOP;
  private static final int HOSTILE_COUNT_Y = 6;
  private static final int HOSTILE_WIDTH = ENTITY_WIDTH - 9;
  
  private static final int SCROLL_LEFT = 174;
  private static final int SCROLL_TOP = 22;
  private static final int SCROLL_WIDTH = 14;
  private static final int SCROLL_HEIGHT = 124;
  
  private final List<IDeity> deityList = new ArrayList<>();  
  private final TabButton[] tabButtons = new TabButton[TAB_COUNT];
  private final List<List<ItemButton>> itemButtons = new ArrayList<>();
  private final List<List<EntityButton>> entityButtons = new ArrayList<>();
  private final List<List<BlessingButton>> blessingButtons = new ArrayList<>();
  private final List<List<HostileButton>> hostileButtons = new ArrayList<>();
  private TabArrowButton tabLeftButton;
  private TabArrowButton tabRightButton;
  private ScrollButton<FavorScreen> scrollButton;
  
  private int tabGroup;
  private int selected;
  
  private Mode mode;
  
  */
/** Number of pixels between left side of screen and left side of gui **//*

  private int guiLeft;
  */
/** Number of pixels between top of screen and top of gui **//*

  private int guiTop;
  */
/** True if there are at least [ITEM_VISIBLE] number of items in the list **//*

  private boolean scrollEnabled;
  
  private IFavor favor;

  public FavorScreen(final DeityContainer screenContainer, final PlayerInventory inv, final ITextComponent title) {
    super(screenContainer, inv, title);
    favor = screenContainer.getFavor();
    // initialize lists (deity, item, entity, etc.)
    if(deityList.isEmpty()) {
      // populate deity list and sort by favor level
      deityList.addAll(GreekFantasy.PROXY.getDeityCollection(true));
      deityList.sort((d1, d2) -> favor.getFavor(d2).compareToAbs(favor.getFavor(d1)));
      
      // populate favor modifier lists for each deity
      for(int deityNum = 0, deityL = deityList.size(); deityNum < deityL; deityNum++) {
        // determine which deity to populate
        final IDeity d = deityList.get(deityNum);
        
        // add item modifier list for each deity
        List<ItemButton> itemButtonList = new ArrayList<>();
        for(final Entry<ResourceLocation, Integer> e : d.getItemFavorModifiers().entrySet()) {
          final Item item = ForgeRegistries.ITEMS.getValue(e.getKey());
          itemButtonList.add(new ItemButton(this, item, e.getValue(), 0, 0));
        }
        itemButtonList.sort(ItemButton::compareTo);
        for(int i = 0, l = itemButtonList.size(); i < l; i++) { itemButtonList.get(i).setIndex(i); }
        itemButtons.add(itemButtonList);
        
        // add entity type modifier list for each deity
        final List<EntityButton> entityButtonList = new ArrayList<>();
        for(final Entry<ResourceLocation, Integer> e : d.getKillFavorModifiers().entrySet()) {
          final EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(e.getKey());
          EntityButton button = new EntityButton(this, entityType, e.getValue(), 0, 0);
          entityButtonList.add(button);
        }
        entityButtonList.sort(EntityButton::compareTo);
        for(int i = 0, l = entityButtonList.size(); i < l; i++) { entityButtonList.get(i).setIndex(i); }
        entityButtons.add(entityButtonList);
        
        // add ability list for each deity
        final List<BlessingButton> blessingButtonList = new ArrayList<>();
        for(final SpecialFavorEffect e : d.getSpecialFavorEffects()) {
          BlessingButton button = new BlessingButton(this, e.getType(), e.getMinLevel(), e.getMaxLevel(), 0, 0);
          blessingButtonList.add(button);
        }
        for(final FavorEffect e : d.getFavorEffects()) {
          BlessingButton button = new BlessingButton(this, e, e.getMinLevel(), e.getMaxLevel(), 0, 0);
          blessingButtonList.add(button);
        }
        blessingButtonList.sort(BlessingButton::compareTo);
        for(int i = 0, l = blessingButtonList.size(); i < l; i++) { blessingButtonList.get(i).setIndex(i); }
        blessingButtons.add(blessingButtonList);

        // add hostile range list for each deity
        hostileButtons.add(new ArrayList<>());
      }
    }
    // create hostile button list for all deities in one list
    final List<HostileButton> hostileButtonList = new ArrayList<>();
    for(final Entry<ResourceLocation, ConfiguredFavorRange> e : GreekFantasy.PROXY.getFavorConfiguration().getEntityTargetMap().entrySet()) {
      if(e.getValue().hasHostileRange()) {
        final EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(e.getKey());
        final FavorRange hostile = e.getValue().getHostileRange();
        HostileButton button = new HostileButton(this, entityType, hostile.getDeity(), hostile.getMinLevel(), hostile.getMaxLevel(), 0, 0);
        hostileButtonList.add(button);
      }
    }
    // place the buttons in different lists based on which deity they belong to
    for(final HostileButton btn : hostileButtonList) {
      int index = deityList.indexOf(btn.deity);
      btn.setIndex(hostileButtons.get(index).size());
      hostileButtons.get(index).add(btn);
    }
    hostileButtons.forEach(l -> l.sort(HostileButton::compareTo));
   
    // default scroll settings
    scrollEnabled = itemButtons.get(selected).size() > (ITEM_COUNT_X * ITEM_COUNT_Y);
  }
  
  @Override
  public void init(Minecraft minecraft, int width, int height) {
    super.init(minecraft, width, height);
    this.guiLeft = (this.width - SCREEN_WIDTH - TAB_WIDTH) / 2;
    this.guiTop = (this.height - (SCREEN_HEIGHT - TAB_HEIGHT)) / 2 - 10;
    this.playerInventoryTitleY = this.height;
    // add 'done' button
    addButton(new Button(guiLeft, guiTop + SCREEN_HEIGHT + 4, SCREEN_WIDTH, 20, new TranslationTextComponent("gui.done"), c -> this.minecraft.displayGuiScreen(null)));
    // add deity tabs
    for(int i = 0, l = Math.min(TAB_COUNT, deityList.size()); i < l; i++) {
      tabButtons[i] = addButton(new TabButton(this, i, deityList.get(i).getText(), guiLeft + (i * TAB_WIDTH), guiTop - TAB_HEIGHT + 4));
    }
    // add deity tab arrow buttons
    tabLeftButton = addButton(new TabArrowButton(this, true, guiLeft - ARROW_WIDTH - 2, guiTop - (ARROW_HEIGHT + 4)));
    tabRightButton = addButton(new TabArrowButton(this, false, guiLeft + SCREEN_WIDTH + 2, guiTop - (ARROW_HEIGHT + 4)));
    // add item buttons
    itemButtons.forEach(l -> l.forEach(b -> addButton(b)));
    // add entity type text components
    entityButtons.forEach(l -> l.forEach(b -> addButton(b)));
    // add ability text components
    blessingButtons.forEach(l -> l.forEach(b -> addButton(b)));
    // add hostile buttons
    hostileButtons.forEach(l -> l.forEach(b -> addButton(b)));
    // add scroll button
    scrollButton = addButton(new ScrollButton<>(this, guiLeft + SCROLL_LEFT, guiTop + SCROLL_TOP, SCROLL_WIDTH, SCROLL_HEIGHT, 
        0, SCREEN_HEIGHT + 2 * BTN_HEIGHT, SCREEN_TEXTURE, s -> s.scrollEnabled, 4, b -> {
          updateScroll(b.getScrollAmount());
        }));
    // add mode buttons
    addButton(new ModeButton(this, guiLeft + BTN_LEFT, guiTop + BTN_TOP, "gui.mirror.item", Mode.ITEM));
    addButton(new ModeButton(this, guiLeft + BTN_LEFT, guiTop + BTN_TOP + BTN_HEIGHT, "gui.mirror.entity", Mode.ENTITY));
    addButton(new ModeButton(this, guiLeft + BTN_LEFT, guiTop + BTN_TOP + (BTN_HEIGHT) * 2, "gui.mirror.blessing", Mode.BLESSING));
    addButton(new ModeButton(this, guiLeft + BTN_LEFT, guiTop + BTN_TOP + (BTN_HEIGHT) * 3, "gui.mirror.hostile", Mode.HOSTILE));
    // set selected deity now that things are nonnull
    int tab = 0;
    int deity = 0;
    if(container.getDeity().isPresent()) {
      // determine which tab/deity to select based on the passed diety name
      for(int i = 0, l = deityList.size(); i < l; i++) {
        if(deityList.get(i).getName().equals(container.getDeity().get())) {
          tab = i / TAB_COUNT;
          deity = i;
          break;
        }
      }
    }
    setSelectedTab(tab);
    setSelectedDeity(deity);
    updateMode(Mode.ITEM);
  }
  
  */
/**
   * Called from the main game loop to update the screen.
   *//*

  @Override
  public void tick() {
    super.tick();
    if(scrollButton != null) {
      scrollButton.tick();
    }
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
    this.font.drawText(matrixStack, new StringTextComponent("** ").appendSibling(deityList.get(selected).getText()).appendString(" **").mergeStyle(TextFormatting.BLACK), 
        this.guiLeft + FAVOR_LEFT, this.guiTop + 5, 0xFFFFFF);
    // draw "favor modifiers" text
    String subtitle = mode.getTooltip();
    this.font.drawText(matrixStack, new TranslationTextComponent(subtitle).mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC), 
        guiLeft + (ITEM_LEFT + ITEM_WIDTH * 2) / 2, guiTop + FAVOR_TOP, 0xFFFFFF);
    // draw favor text
    drawFavorText(matrixStack, mouseX, mouseY);        
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
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
    double multiplier = 1.0D;
    if(mode == Mode.ITEM) {
      multiplier = 1.0F / (itemButtons.get(selected).size() / ITEM_COUNT_X);
    } else if(mode == Mode.ENTITY) {
      multiplier = 1.0F / entityButtons.get(selected).size();
    } else if(mode == Mode.BLESSING) {
      multiplier = 1.0F / blessingButtons.get(selected).size();
    } else if(mode == Mode.HOSTILE) {
      multiplier = 1.0F / hostileButtons.get(selected).size();
    }
    return this.scrollButton.mouseScrolled(mouseX, mouseY, scrollAmount * multiplier);
 }
  
  private void drawFavorText(MatrixStack matrixStack, int mouseX, int mouseY) {
    // draw favor values
    final FavorLevel level = favor.getFavor(deityList.get(selected));
    this.font.drawText(matrixStack, new TranslationTextComponent("favor.favor").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC), 
        guiLeft + FAVOR_LEFT, guiTop + FAVOR_TOP, 0xFFFFFF);
    final long curFavor = level.getFavor();
    final long nextFavor = level.getFavorToNextLevel();
    this.font.drawText(matrixStack, new StringTextComponent(String.valueOf(curFavor + " / " + nextFavor))
        .mergeStyle(TextFormatting.DARK_PURPLE), 
        guiLeft + FAVOR_LEFT, guiTop + FAVOR_TOP + font.FONT_HEIGHT * 1 + 1, 0xFFFFFF);
    this.font.drawText(matrixStack, new TranslationTextComponent("favor.level").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC), 
        guiLeft + FAVOR_LEFT, guiTop + FAVOR_TOP + font.FONT_HEIGHT * 5 / 2, 0xFFFFFF);
    this.font.drawText(matrixStack, new StringTextComponent(String.valueOf(level.getLevel() + " / " + (curFavor < 0 ? level.getMinLevel() : level.getMaxLevel()))).mergeStyle(TextFormatting.DARK_PURPLE), 
        guiLeft + FAVOR_LEFT, guiTop + FAVOR_TOP + font.FONT_HEIGHT * 7 / 2 + 1, 0xFFFFFF);
    this.font.drawText(matrixStack, new TranslationTextComponent("favor.next_level").mergeStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC), 
        guiLeft + FAVOR_LEFT, guiTop + FAVOR_TOP + font.FONT_HEIGHT * 5, 0xFFFFFF);
    final boolean capped = level.getLevel() == level.getMinLevel() || level.getLevel() == level.getMaxLevel();
    this.font.drawText(matrixStack, new StringTextComponent(capped ? "--" : String.valueOf(nextFavor - curFavor)).mergeStyle(TextFormatting.DARK_PURPLE), 
        guiLeft + FAVOR_LEFT, guiTop + FAVOR_TOP + font.FONT_HEIGHT * 6 + 1, 0xFFFFFF);
  }
  
  private void setSelectedTab(int index) {
    tabGroup = index;
    for(int i = 0; i < TAB_COUNT; i++) {
      if(tabButtons[i] != null) {
        tabButtons[i].updateDeity(i + tabGroup * TAB_COUNT);
      }
    }
    // show/hide tab arrows
//    tabLeftButton.visible = index > 0;
//    tabRightButton.visible = index < tabButtons.length / TAB_COUNT;
  }
  
  private void setSelectedDeity(int index) {
    selected = index;
    updateMode(mode);
  }
  
  protected void updateMode(final Mode modeIn) {
    mode = modeIn;
    if(modeIn == Mode.ITEM) {
      scrollEnabled = itemButtons.get(selected).size() > (ITEM_COUNT_X * ITEM_COUNT_Y);
    } else if(modeIn == Mode.ENTITY) {
      scrollEnabled = entityButtons.get(selected).size() > ENTITY_COUNT_Y;
    } else if(modeIn == Mode.BLESSING) {
      scrollEnabled = blessingButtons.get(selected).size() > BLESSING_COUNT_Y;
    } else if(modeIn == Mode.HOSTILE) {
      scrollEnabled = hostileButtons.get(selected).size() > HOSTILE_COUNT_Y;
    }
    // hide / show the proper item button lists
    for(int i = 0, l = itemButtons.size(); i < l; i++) {
      final boolean buttonsVisible = (i == selected && modeIn == Mode.ITEM);
      itemButtons.get(i).forEach(b -> b.visible = buttonsVisible);
    }
    // hide / show the proper entity button lists
    for(int i = 0, l = entityButtons.size(); i < l; i++) {
      final boolean buttonsVisible = (i == selected && modeIn == Mode.ENTITY);
      entityButtons.get(i).forEach(b -> b.visible = buttonsVisible);
    }
    // hide / show the proper entity button lists
    for(int i = 0, l = blessingButtons.size(); i < l; i++) {
      final boolean buttonsVisible = (i == selected && modeIn == Mode.BLESSING);
      blessingButtons.get(i).forEach(b -> b.visible = buttonsVisible);
    }
    // hide / show the proper hostile button lists
    for(int i = 0, l = hostileButtons.size(); i < l; i++) {
      final boolean buttonsVisible = (i == selected && modeIn == Mode.HOSTILE);
      hostileButtons.get(i).forEach(b -> b.visible = buttonsVisible);
    }
    scrollButton.resetScroll();
  }

  protected void updateScroll(final float amount) {
    if(mode == Mode.ITEM) {
      final int startIndex = (int) Math.round(amount * ((itemButtons.get(selected).size() / ITEM_COUNT_X) - ITEM_COUNT_Y + 1));
      itemButtons.get(selected).forEach(b -> b.updateLocation(startIndex));
    } else if(mode == Mode.ENTITY) {
      final int startIndex = (int) Math.round(amount * (entityButtons.get(selected).size() - ENTITY_COUNT_Y));
      entityButtons.get(selected).forEach(b -> b.updateLocation(startIndex));
    } else if(mode == Mode.BLESSING) {
      final int startIndex = (int) Math.round(amount * (blessingButtons.get(selected).size() - BLESSING_COUNT_Y));
      blessingButtons.get(selected).forEach(b -> b.updateLocation(startIndex));
    } else if(mode == Mode.HOSTILE) {
      final int startIndex = (int) Math.round(amount * (hostileButtons.get(selected).size() - HOSTILE_COUNT_Y));
      hostileButtons.get(selected).forEach(b -> b.updateLocation(startIndex));
    }
  }
  
  protected class ItemButton extends Button implements Comparable<ItemButton> {
    
    private final ItemStack item;
    private final int value;
    
    private int index;
    private ITextComponent valueText = StringTextComponent.EMPTY;
    
    public ItemButton(final FavorScreen gui, final Item itemIn, final int itemValue, final int x, final int y) {
      super(x, y, ITEM_WIDTH, ITEM_HEIGHT, StringTextComponent.EMPTY, b -> {}, (b, m, bx, by) -> gui.renderTooltip(m, b.getMessage(), bx, by));
      this.visible = false;
      this.visible = true;
      item = new ItemStack(itemIn);
      value = itemValue;
      final String itemValueString = ((itemValue > 0) ? "+" : "") + itemValue;
      final TextFormatting color = (itemValue > 0) ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED;
      valueText = new StringTextComponent(itemValueString).mergeStyle(color);
      this.setMessage(new TranslationTextComponent(item.getTranslationKey()).appendSibling(new StringTextComponent(" " + itemValueString).mergeStyle(color)));
    }

    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible) {
        // draw item
        FavorScreen.this.itemRenderer.renderItemIntoGUI(item, this.x + 1, this.y + 1);
        // draw string
        FavorScreen.this.font.drawText(matrixStack, valueText, this.x + 20, this.y + (1 + FavorScreen.this.font.FONT_HEIGHT) / 2, 0xFFFFFF);
      }
    }
    
    public void setIndex(final int i) {
      index = i;
    }

    */
/**
     * Determines whether this button should show on the screen, and if so,
     * what y-position to assign in order to align with the current scroll value.
     * @param startIndex the start index of the buttons that are currently visible,
     * based on the scroll value
     **//*

    public void updateLocation(final int startIndex) {
      final int iIndex = (index / FavorScreen.ITEM_COUNT_X);
      final int sIndex = (startIndex / FavorScreen.ITEM_COUNT_X) * FavorScreen.ITEM_COUNT_X;
      if(iIndex < sIndex || iIndex >= (sIndex + FavorScreen.ITEM_COUNT_Y)) {
        this.visible = false;
        this.isHovered = false;
      } else {
        this.visible = true;
        this.x = FavorScreen.this.guiLeft + FavorScreen.ITEM_LEFT + (index % 2) * ITEM_WIDTH;
        this.y = FavorScreen.this.guiTop + FavorScreen.ITEM_TOP + FavorScreen.ITEM_HEIGHT * (iIndex - sIndex);
      }
    }
    
    @Override
    public int compareTo(final ItemButton button) {
      return button.value - this.value;
    }

  }
  
  protected class EntityButton extends Button implements Comparable<EntityButton> {
    
    private final int value;
    private int index;
    private int rightAlign = -1;
    
    public EntityButton(final FavorScreen gui, final EntityType<?> entityIn, final int entityValue, final int x, final int y) {
      super(x, y, ENTITY_WIDTH, 9, StringTextComponent.EMPTY, b -> {});
      value = entityValue;
      final TextFormatting color = entityValue < 0 ? TextFormatting.DARK_RED : TextFormatting.DARK_GREEN;
      this.setMessage(new TranslationTextComponent(entityIn.getTranslationKey()).mergeStyle(TextFormatting.BLACK)
          .appendSibling(new StringTextComponent(":").mergeStyle(TextFormatting.BLACK))
          .appendSibling(new StringTextComponent(String.format("%4s", (entityValue < 0 ? "" : "+") + entityValue)).mergeStyle(color)));
    }
    
    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible) {
        FavorScreen.this.font.drawText(matrixStack, this.getMessage(), this.x, this.y, 0xFFFFFF);
      }
    }
    
    public void setIndex(final int i) {
      index = i;
    }
    
    */
/**
     * Determines whether this button should show on the screen, and if so,
     * what y-position to assign in order to align with the current scroll value.
     * @param startIndex the start index of the buttons that are currently visible,
     * based on the scroll value
     **//*

    public void updateLocation(final int startIndex) {
      if(index < startIndex || index >= (startIndex + FavorScreen.ENTITY_COUNT_Y)) {
        this.visible = false;
        this.isHovered = false;
      } else {
        this.visible = true;
        this.y = FavorScreen.this.guiTop + FavorScreen.ENTITY_TOP + FavorScreen.this.font.FONT_HEIGHT * (index - startIndex);
        if(rightAlign < 0) {
          rightAlign = ENTITY_WIDTH - FavorScreen.this.font.getStringWidth(getMessage().getString());
        }
        this.x = FavorScreen.this.guiLeft + FavorScreen.ENTITY_LEFT + rightAlign;
      }
    }

    @Override
    public int compareTo(EntityButton button) {
      return button.value - this.value;
    }
  }
  
  protected class HostileButton extends Button implements Comparable<HostileButton> {
    private final IDeity deity;
    private final int minValue;
    private final int maxValue;
    private int index;
    private final ITextComponent entityName;
    private final ITextComponent entityValue;
    
    public HostileButton(final FavorScreen gui, final EntityType<?> entityIn, final IDeity deityIn, final int minLevel, final int maxLevel, final int x, final int y) {
      super(x, y, HOSTILE_WIDTH, 18, StringTextComponent.EMPTY, b -> {});
      deity = deityIn;
      minValue = minLevel;
      maxValue = maxLevel;
      entityName = new TranslationTextComponent(entityIn.getTranslationKey()).mergeStyle(TextFormatting.BLACK)
          .appendSibling(new StringTextComponent(":").mergeStyle(TextFormatting.BLACK));
      entityValue = new TranslationTextComponent("favor.level_range", minLevel, maxLevel).mergeStyle(TextFormatting.DARK_RED);
    }
    
    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible) {
        FavorScreen.this.font.drawText(matrixStack, entityName, this.x, this.y, 0xFFFFFF);
        FavorScreen.this.font.drawText(matrixStack, entityValue, this.x + 9, this.y + FavorScreen.this.font.FONT_HEIGHT, 0xFFFFFF);
      }
    }
    
    public void setIndex(final int i) {
      index = i;
    }
    
    */
/**
     * Determines whether this button should show on the screen, and if so,
     * what y-position to assign in order to align with the current scroll value.
     * @param startIndex the start index of the buttons that are currently visible,
     * based on the scroll value
     **//*

    public void updateLocation(final int startIndex) {
      if(index < startIndex || index >= (startIndex + FavorScreen.HOSTILE_COUNT_Y)) {
        this.visible = false;
        this.isHovered = false;
      } else {
        this.visible = true;
        this.y = FavorScreen.this.guiTop + FavorScreen.HOSTILE_TOP + 2 * FavorScreen.this.font.FONT_HEIGHT * (index - startIndex);
        this.x = FavorScreen.this.guiLeft + FavorScreen.HOSTILE_LEFT;
      }
    }

    @Override
    public int compareTo(HostileButton button) {
      return button.minValue - this.minValue;
    }
  }
  
  protected class BlessingButton extends Button implements Comparable<BlessingButton> {
    private final int minValue;
    private final int maxValue;
    private int index;
    private final ITextComponent textHeader;
    private final ITextComponent textRange;
    
    public BlessingButton(final FavorScreen gui, final SpecialFavorEffect.Type typeIn, final int minLevel, final int maxLevel, final int x, final int y) {
      super(x, y, HOSTILE_WIDTH, 18, StringTextComponent.EMPTY, b -> {});
      minValue = minLevel;
      maxValue = maxLevel;
      textHeader = new TranslationTextComponent(typeIn.getTranslationKey()).mergeStyle(TextFormatting.BLACK)
          .appendSibling(new StringTextComponent(":").mergeStyle(TextFormatting.BLACK));
      TextFormatting color = maxLevel > 0 ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED;
      textRange = new TranslationTextComponent("favor.level_range", minLevel, maxLevel).mergeStyle(color);
    }
    
    public BlessingButton(final FavorScreen gui, final FavorEffect effectIn, final int minLevel, final int maxLevel, final int x, final int y) {
      super(x, y, HOSTILE_WIDTH, 18, StringTextComponent.EMPTY, b -> {});
      minValue = minLevel;
      maxValue = maxLevel;
      textHeader = new TranslationTextComponent(effectIn.getTranslationKey()).mergeStyle(TextFormatting.BLACK)
          .appendSibling(new StringTextComponent(":").mergeStyle(TextFormatting.BLACK));
      TextFormatting color = maxLevel > 0 ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED;
      textRange = new TranslationTextComponent("favor.level_range", minLevel, maxLevel).mergeStyle(color);
    }
    
    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible) {
        FavorScreen.this.font.drawText(matrixStack, textHeader, this.x, this.y, 0xFFFFFF);
        FavorScreen.this.font.drawText(matrixStack, textRange, this.x + 9, this.y + FavorScreen.this.font.FONT_HEIGHT, 0xFFFFFF);
      }
    }
    
    public void setIndex(final int i) {
      index = i;
    }
    
    */
/**
     * Determines whether this button should show on the screen, and if so,
     * what y-position to assign in order to align with the current scroll value.
     * @param startIndex the start index of the buttons that are currently visible,
     * based on the scroll value
     **//*

    public void updateLocation(final int startIndex) {
      if(index < startIndex || index >= (startIndex + FavorScreen.BLESSING_COUNT_Y)) {
        this.visible = false;
        this.isHovered = false;
      } else {
        this.visible = true;
        this.y = FavorScreen.this.guiTop + FavorScreen.BLESSING_TOP + 2 * FavorScreen.this.font.FONT_HEIGHT * (index - startIndex);
        this.x = FavorScreen.this.guiLeft + FavorScreen.BLESSING_LEFT;
      }
    }

    @Override
    public int compareTo(BlessingButton button) {
      return button.minValue - this.minValue;
    }
  }
  
  protected class TabButton extends Button {
    
    private int id;
    private ItemStack item = ItemStack.EMPTY;

    public TabButton(final FavorScreen gui, final int index, final ITextComponent title, final int x, final int y) {
      super(x, y, TAB_WIDTH, TAB_HEIGHT, title, b -> gui.setSelectedDeity(((TabButton)b).id), (b, m, bx, by) -> gui.renderTooltip(m, b.getMessage(), bx, by));
      updateDeity(index);
    }
    
    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible) {
        int selected = isSelected() ? 0 : 2;
        final int xOffset = (id % TAB_COUNT) * TAB_WIDTH;
        final int yOffset = isSelected() ? this.height : 2;
        // draw button background
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        FavorScreen.this.getMinecraft().getTextureManager().bindTexture(TABS_TEXTURE);
        this.blit(matrixStack, this.x, this.y - selected, xOffset, yOffset - selected, this.width, this.height - selected);
        // draw item
        FavorScreen.this.itemRenderer.renderItemIntoGUI(item, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
      }
    }
    
    public void updateDeity(final int deityId) {
      id = deityId;
      if(id < FavorScreen.this.deityList.size()) {
        this.visible = true;
        this.setMessage(deityList.get(id).getText());
        final ResourceLocation rl = FavorScreen.this.deityList.get(deityId).getName();
        final ResourceLocation altar = new ResourceLocation(rl.getNamespace(), "altar_" + rl.getPath());
        item = new ItemStack(ForgeRegistries.ITEMS.containsKey(altar) ? ForgeRegistries.ITEMS.getValue(altar) : GFRegistry.PANFLUTE);
      } else {
        this.visible = false;
      }
    }
    
    public boolean isSelected() {
      return FavorScreen.this.selected == id;
    }
  }
  
  protected class TabArrowButton extends Button {
    
    private final boolean isLeft;

    public TabArrowButton(final FavorScreen gui, final boolean left, final int x, final int y) {
      super(x, y, ARROW_WIDTH, ARROW_HEIGHT, StringTextComponent.EMPTY, b -> gui.setSelectedTab(left ? Math.max(0, gui.tabGroup - 1) : Math.min(gui.deityList.size() / TAB_COUNT, gui.tabGroup + 1)));
      isLeft = left;
    }
    
    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible && FavorScreen.this.deityList.size() > TAB_COUNT) {
        final int xOffset = isLeft ? ARROW_WIDTH : 0;
        final int yOffset = 128 + (isHovered ? ARROW_HEIGHT : 0);
        // draw button background
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        FavorScreen.this.getMinecraft().getTextureManager().bindTexture(TABS_TEXTURE);
        this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
      }
    }
  }
  
  protected class ModeButton extends Button {
    
    private final Mode screenMode;
    private final ITextComponent tooltip;
   
    public ModeButton(final FavorScreen screenIn, final int x, final int y, final String translationKey, final Mode modeIn) {
      super(x, y, BTN_WIDTH, BTN_HEIGHT, new TranslationTextComponent(translationKey), b -> screenIn.updateMode(modeIn), (b, m, bx, by) -> screenIn.renderTooltip(m, ((ModeButton)b).tooltip, bx, by));
      screenMode = modeIn;
      tooltip = new TranslationTextComponent(translationKey.concat(".tooltip"));
    }

    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if(this.visible) {
        final boolean selected = isSelected();
        final int xOffset = 0;
        final int yOffset = SCREEN_HEIGHT + (selected ? this.height : 0);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        FavorScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_TEXTURE);
        this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
        drawCenteredString(matrixStack, FavorScreen.this.font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
      }
    }
    
    protected boolean isSelected() {
      return this.isHovered() || FavorScreen.this.mode == screenMode;
    }
  }
  
  protected static enum Mode {
    ITEM("favor.favor_modifiers"), 
    ENTITY("favor.favor_modifiers"), 
    BLESSING("gui.mirror.blessing_levels"), 
    HOSTILE("gui.mirror.hostility_levels");
    
    private String tooltip;
    
    private Mode(final String tooltipIn) {
      tooltip = tooltipIn;
    }
    
    public String getTooltip() { return tooltip; }
  }
}
*/
