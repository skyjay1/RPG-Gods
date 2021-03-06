package rpggods.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import rpggods.RPGGods;
import rpggods.altar.AltarItems;
import rpggods.altar.AltarPose;
import rpggods.altar.ModelPart;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;
import rpggods.gui.AltarContainer;
import rpggods.network.CUpdateAltarPacket;

import java.util.Optional;

public class AltarScreen extends ContainerScreen<AltarContainer> {

    // CONSTANTS
    private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/gui/altar/altar.png");
    private static final ResourceLocation SCREEN_WIDGETS = new ResourceLocation(RPGGods.MODID, "textures/gui/altar/altar_widgets.png");

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_COUNT = 2;

    private static final int SCREEN_WIDTH = 224;
    private static final int SCREEN_HEIGHT = 202;

    private static final int PREVIEW_WIDTH = 54;
    private static final int PREVIEW_HEIGHT = 80;
    private static final int PREVIEW_X = 7;
    private static final int PREVIEW_Y = 7;

    private static final int PARTS_X = 147;
    private static final int PARTS_Y = 7;

    private static final int GENDER_X = 7;
    private static final int GENDER_Y = 87;
    private static final int SLIM_X = 45;
    private static final int SLIM_Y = 87;
    private static final int PRESET_X = 104;
    private static final int PRESET_Y = 87;
    private static final int RESET_X = 123;
    private static final int RESET_Y = 87;

    private static final int SLIDER_X = 69;
    private static final int SLIDER_Y = 14;
    private static final int SLIDER_WIDTH = 70;
    private static final int SLIDER_HEIGHT = 20;
    private static final int SLIDER_SPACING = 4;

    private static final int PART_WIDTH = 70;
    private static final int PART_HEIGHT = 14;

    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    private static final int TEXT_X = 100;
    private static final int TEXT_Y = 7;
    private static final int TEXT_WIDTH = 117;
    private static final int TEXT_HEIGHT = 16;

    private final AltarScreen.TabButton[] tabButtons = new AltarScreen.TabButton[TAB_COUNT];
    private int tabIndex;

    private boolean enabled;
    private Optional<String> name;
    private boolean female;
    private boolean slim;
    private AltarItems items;
    private AltarPose pose;
    private boolean poseLocked;

    protected AngleSlider sliderAngleX;
    protected AngleSlider sliderAngleY;
    protected AngleSlider sliderAngleZ;
    private final PartButton[] partButtons = new PartButton[ModelPart.values().length];
    private IconButton genderButton;
    private IconButton slimButton;
    private IconButton presetButton;
    private IconButton resetButton;
    private ModelPart selectedPart = ModelPart.BODY;

    private TextFieldWidget nameField;

    public AltarScreen(final AltarContainer screenContainer, final PlayerInventory inv, final ITextComponent title) {
        super(screenContainer, inv, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT - TAB_HEIGHT / 2;
        this.inventoryLabelX = this.leftPos + AltarContainer.PLAYER_INV_X;
        this.inventoryLabelY = this.topPos + AltarContainer.PLAYER_INV_Y - 10;
        Altar altar = screenContainer.getAltar();
        enabled = altar.isEnabled();
        if(altar.getDeity().isPresent()) {
            name = Optional.empty();
        } else if(screenContainer.getEntity().hasCustomName()) {
            name = Optional.of(screenContainer.getEntity().getCustomName().getContents());
        } else {
            name = Optional.empty();
        }
        female = altar.isFemale();
        slim = altar.isSlim();
        items = altar.getItems();
        pose = altar.getPose();
        poseLocked = altar.isPoseLocked();
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        // add tab buttons
        tabButtons[0] = addButton(new AltarScreen.TabButton(this, 0, new TranslationTextComponent("gui.altar.pose"),
                leftPos + (0 * TAB_WIDTH), topPos - TAB_HEIGHT + 4, new ItemStack(Items.ARMOR_STAND)));
        tabButtons[1] = addButton(new AltarScreen.TabButton(this, 1, new TranslationTextComponent("gui.altar.items"),
                leftPos + (1 * TAB_WIDTH), topPos - TAB_HEIGHT + 4, new ItemStack(Items.IRON_SWORD)));
        // add part buttons
        for (int i = 0, l = ModelPart.values().length; i < l; i++) {
            final ModelPart p = ModelPart.values()[i];
            final ITextComponent title = new TranslationTextComponent("gui.altar." + p.getSerializedName());
            partButtons[i] = this.addButton(new PartButton(this, this.leftPos + PARTS_X, this.topPos + PARTS_Y + (PART_HEIGHT * i), title, button -> {
                this.selectedPart = p;
                AltarScreen.this.updateSliders();
            }) {
                @Override
                protected boolean isSelected() {
                    return this.isHovered() || (p == AltarScreen.this.selectedPart);
                }
            });
        }
        // add randomize button
        final ITextComponent titlePreset = new TranslationTextComponent("gui.altar.preset");
        final AltarPose[] presets = new AltarPose[] { AltarPose.STANDING_HOLDING, AltarPose.STANDING_RAISED,
                AltarPose.STANDING_HOLDING_DRAMATIC, AltarPose.WALKING, AltarPose.WEEPING, AltarPose.DAB };
        presetButton = this.addButton(new IconButton(this, this.leftPos + PRESET_X, this.topPos + PRESET_Y, 16, 202, titlePreset, button -> {
            this.pose = presets[(int)Math.floor(Math.random() * presets.length)];
        }));
        // add reset button
        final ITextComponent titleReset = new TranslationTextComponent("controls.reset");
        resetButton = this.addButton(new IconButton(this, this.leftPos + RESET_X, this.topPos + RESET_Y, 0, 202, titleReset, button -> {
            AltarScreen.this.pose.set(AltarScreen.this.selectedPart, 0, 0, 0);
            AltarScreen.this.updateSliders();
        }));
        // add gender button
        final ITextComponent titleGender = new TranslationTextComponent("gui.altar.gender");
        genderButton = this.addButton(new IconButton(this, this.leftPos + GENDER_X, this.topPos + GENDER_Y, 0, 218, titleGender, button -> AltarScreen.this.female = !AltarScreen.this.female) {
            @Override
            public int getIconX() {
                return super.getIconX() + (AltarScreen.this.female ? 0 : this.width);
            }
        });
        // add slim button
        final ITextComponent titleSlim = new TranslationTextComponent("gui.altar.slim");
        slimButton = this.addButton(new IconButton(this, this.leftPos + SLIM_X, this.topPos + SLIM_Y, 32, 218, titleSlim, button -> AltarScreen.this.slim = !AltarScreen.this.slim) {
            @Override
            public int getIconX() {
                return super.getIconX() + (AltarScreen.this.slim ? 0 : this.width);
            }
        });
        // add sliders
        this.sliderAngleX = (new AltarScreen.AngleSlider(this.leftPos + SLIDER_X, this.topPos + SLIDER_Y, "X") {
            @Override
            void setAngleValue(double angRadians) { AltarScreen.this.pose.get(AltarScreen.this.selectedPart).setX((float)angRadians); }
            @Override
            double getAngleValue() { return Math.toDegrees(AltarScreen.this.pose.get(AltarScreen.this.selectedPart).x()); }
        });
        this.sliderAngleY = (new AltarScreen.AngleSlider(this.leftPos + SLIDER_X, this.topPos + SLIDER_Y + (SLIDER_HEIGHT + SLIDER_SPACING), "Y") {
            @Override
            void setAngleValue(double angRadians) { AltarScreen.this.pose.get(AltarScreen.this.selectedPart).setY((float)angRadians); }
            @Override
            double getAngleValue() { return Math.toDegrees(AltarScreen.this.pose.get(AltarScreen.this.selectedPart).y()); }
        });
        this.sliderAngleZ = (new AltarScreen.AngleSlider(this.leftPos + SLIDER_X, this.topPos + SLIDER_Y + 2 * (SLIDER_HEIGHT + SLIDER_SPACING), "Z") {
            @Override
            void setAngleValue(double angRadians) { AltarScreen.this.pose.get(AltarScreen.this.selectedPart).setZ((float)angRadians); }
            @Override
            double getAngleValue() { return Math.toDegrees(AltarScreen.this.pose.get(AltarScreen.this.selectedPart).z()); }
        });
        this.addButton(sliderAngleX);
        this.addButton(sliderAngleY);
        this.addButton(sliderAngleZ);
        // items tab
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.nameField = new TextFieldWidget(this.font, this.leftPos + TEXT_X, this.topPos + TEXT_Y, TEXT_WIDTH, TEXT_HEIGHT, new TranslationTextComponent("gui.altar.name"));
        this.nameField.setValue(name.orElse(""));
        this.nameField.setCanLoseFocus(true);
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setBordered(true);
        this.nameField.setMaxLength(35);
        this.nameField.setResponder(s -> name = (s != null && s.length() > 0) ? Optional.of(s) : Optional.empty());
        this.nameField.setEditable(!getMenu().getEntity().isNameLocked());
        this.children.add(this.nameField);
        // update
        this.updateSliders();
        this.updateTab(0);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        this.renderBackground(matrixStack);
        RenderHelper.setupForFlatItems();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // draw background
        this.minecraft.getTextureManager().bind(SCREEN_TEXTURE);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        // draw item slots
        if(this.tabIndex == 1) {
            // slots on texture are at 31, 119
            for(int i = 0, l = getMenu().getAltarSlots().size(); i < l; i++) {
                Slot slot = getMenu().getAltarSlots().get(i);
                this.blit(matrixStack, this.leftPos + slot.x - 1, this.topPos + slot.y - 1,
                        AltarContainer.PLAYER_INV_X - 1, AltarContainer.PLAYER_INV_Y - 1, 18, 18);
                // render slab icon on last slot
                if(i == l - 1 && slot.getItem().isEmpty()) {
                    this.blit(matrixStack, this.leftPos + slot.x, this.topPos + slot.y,
                            48, 202, 16, 16);
                }
            }
        }
        // draw preview pane
        this.minecraft.getTextureManager().bind(SCREEN_WIDGETS);
        this.blit(matrixStack, this.leftPos + PREVIEW_X, this.topPos + PREVIEW_Y, 168, 130, PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw entity preview
        drawEntityOnScreen(matrixStack, this.leftPos + PREVIEW_X + 12, this.topPos + PREVIEW_Y + 4, mouseX, mouseY, partialTicks);
        // draw text box
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw hovering text LAST
        for (final Widget b : this.buttons) {
            if (b.visible && b.isHovered()) {
                b.renderToolTip(matrixStack, mouseX, mouseY);
            }
        }
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void tick() {
        super.tick();
        this.nameField.tick();
    }

    @Override
    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
        this.menu.setChanged();
        // send update packet to server
        RPGGods.CHANNEL.sendToServer(new CUpdateAltarPacket(this.menu.getEntity().getId(), this.pose, this.female, this.slim, this.name.orElse("")));
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.nameField.getValue();
        this.init(minecraft, width, height);
        this.nameField.setValue(s);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeContainer();
        }

        return !this.nameField.keyPressed(keyCode, scanCode, modifiers) && !this.nameField.canConsumeInput() ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    protected void updateSliders() {
        if (sliderAngleX != null) {
            this.sliderAngleX.updateSlider();
            this.sliderAngleY.updateSlider();
            this.sliderAngleZ.updateSlider();
        }
    }

    protected void updateTab(final int tabIndex) {
        this.tabIndex = tabIndex;
        boolean tab0 = tabIndex == 0;
        boolean tab1 = tabIndex == 1;
        boolean tab0AndPose = tab0 && !poseLocked;
        sliderAngleX.visible = tab0AndPose;
        sliderAngleY.visible = tab0AndPose;
        sliderAngleZ.visible = tab0AndPose;
        genderButton.visible = tab0AndPose;
        slimButton.visible = tab0AndPose;
        presetButton.visible = tab0AndPose;
        resetButton.visible = tab0AndPose;
        for(PartButton pb : partButtons) {
            pb.visible = tab0AndPose;
        }
        nameField.visible = tab1;
        this.setFocused(tab1 ? this.nameField : null);
        // show/hide inventory
        for (Slot slot : this.getMenu().slots) {
            if(slot instanceof AltarContainer.AltarSlot) {
                ((AltarContainer.AltarSlot)slot).setHidden(tab0);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void drawEntityOnScreen(final MatrixStack matrixStackIn, final int posX, final int posY,
                                       final float mouseX, final float mouseY, final float partialTicks) {
        float margin = 12;
        float scale = PREVIEW_WIDTH - margin * 2;
        float rotX = (float) Math.atan((double) ((mouseX - this.leftPos) / 40.0F));
        float rotY = (float) Math.atan((double) ((mouseY - this.topPos - PREVIEW_HEIGHT / 2) / 40.0F));
        // preview client-side tile entity information
        updateAltarEntity(menu.getEntity());

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
        // apply negative body rotation to ensure entity faces camera
        final float entityYRot = 360.0F - getMenu().getEntity().yBodyRot;
        RenderSystem.rotatef(entityYRot, 0.0F, -1.0F, 0.0F);
        RenderSystem.scalef(1.0F, -1.0F, 1.0F);
        RenderSystem.scalef(scale, scale, scale);
        RenderSystem.rotatef(rotX * 15.0F, 0.0F, 1.0F, 0.0F);
        RenderSystem.rotatef(rotY * 15.0F, 1.0F, 0.0F, 0.0F);

        RenderHelper.setupForFlatItems();

        IRenderTypeBuffer.Impl bufferType = minecraft.renderBuffers().bufferSource();
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(menu.getEntity())
                        .render(menu.getEntity(), 0F, partialTicks, matrixStackIn, bufferType, 15728880);
        bufferType.endBatch();

        RenderSystem.enableDepthTest();
        RenderHelper.setupFor3DItems();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
        RenderSystem.popMatrix();
    }

    /**
     * Updates the client-side model for rendering in the GUI only.
     * Does not send anything to the server.
     *
     * @param entity the AltarEntity to update
     **/
    private void updateAltarEntity(final AltarEntity entity) {
        if(!poseLocked) {
            entity.setFemale(female);
            entity.setSlim(slim);
            entity.setAltarPose(this.pose);
        }
        if(name.isPresent() && !nameField.isFocused() &&
                (null == entity.getPlayerProfile() || !entity.getCustomName().getContents().equals(name.get()))) {
            entity.setCustomName(new StringTextComponent(name.get()));
        }
    }

    protected class TabButton extends Button {

        private int index;
        private ItemStack item = ItemStack.EMPTY;

        public TabButton(final AltarScreen screenIn, final int index, final ITextComponent title, final int x, final int y, ItemStack item) {
            super(x, y, TAB_WIDTH, TAB_HEIGHT, title, b -> screenIn.updateTab(index),
                    (b, m, bx, by) -> screenIn.renderTooltip(m, b.getMessage(), bx, by));
            this.index = index;
            this.item = item;
            this.setMessage(title);
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                int selected = isSelected() ? 0 : 2;
                final int xOffset = (index % TAB_COUNT) * TAB_WIDTH;
                final int yOffset = isSelected() ? this.height : 2;
                // draw button background
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                AltarScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y - selected, xOffset, yOffset - selected, this.width, this.height - selected);
                // draw item
                AltarScreen.this.itemRenderer.renderGuiItem(item, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
            }
        }

        public boolean isSelected() {
            return AltarScreen.this.tabIndex == index;
        }
    }


    protected class PartButton extends Button {

        public PartButton(final AltarScreen screenIn, final int x, final int y, final ITextComponent title, final IPressable pressedAction) {
            super(x, y, PART_WIDTH, PART_HEIGHT, title, pressedAction);
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                final boolean selected = isSelected();
                final int xOffset = 25;
                final int yOffset = 130 + (selected ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                AltarScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
                drawCenteredString(matrixStack, AltarScreen.this.font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
            }
        }

        protected boolean isSelected() {
            return this.isHovered();
        }
    }

    protected class IconButton extends Button {

        private final int textureX;
        private final int textureY;

        public IconButton(final AltarScreen screenIn, final int x, final int y, final int tX, final int tY,
                          final ITextComponent title, final IPressable pressedAction) {
            super(x, y, ICON_WIDTH, ICON_HEIGHT, StringTextComponent.EMPTY, pressedAction,
                    (b, m, bx, by) -> screenIn.renderTooltip(m, screenIn.minecraft.font.split(title, Math.max(screenIn.width / 2 - 43, 170)), bx, by));
            this.textureX = tX;
            this.textureY = tY;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                int xOffset = 97;
                int yOffset = 130 + (this.isHovered() ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                // draw button background
                AltarScreen.this.getMinecraft().getTextureManager().bind(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
                // draw button icon
                AltarScreen.this.getMinecraft().getTextureManager().bind(SCREEN_TEXTURE);
                this.blit(matrixStack, this.x, this.y, getIconX(), getIconY(), this.width, this.height);
            }
        }

        public int getIconX() {
            return textureX;
        }

        public int getIconY() {
            return textureY;
        }
    }

    protected abstract class AngleSlider extends AbstractSlider {

        private final String rotationName;

        public AngleSlider(final int x, final int y, final String rName) {
            super(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, StringTextComponent.EMPTY, 0.5D);
            rotationName = rName;
            this.updateMessage();
        }

        // called when the value is changed
        protected void updateMessage() {
            this.setMessage(new TranslationTextComponent("gui.altar.rotation", rotationName, Math.round(getAngleValue())));
        }

        // called when the value is changed and is different from its previous value
        protected void applyValue() {
            setAngleValue(Math.toRadians((this.value - 0.5D) * getAngleBounds()));
        }

        protected double getValueRadians() {
            return Math.toRadians((this.value - 0.5D) * getAngleBounds());
        }

        public void updateSlider() {
            this.value = MathHelper.clamp((getAngleValue() / getAngleBounds()) + 0.5D, 0.0D, 1.0D);
            this.updateMessage();
        }

        /**
         * @return the range of angles that the slider outputs, in degrees
         **/
        protected double getAngleBounds() {
            return 360.0D;
        }

        /**
         * @return the angle to display to the user, in degrees
         **/
        abstract double getAngleValue();

        /**
         * Updates this slider's value with an angle in radians
         *
         * @param angRadians the angle in radians
         **/
        abstract void setAngleValue(final double angRadians);
    }

}
