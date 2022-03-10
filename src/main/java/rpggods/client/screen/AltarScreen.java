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
    private static final ResourceLocation SCREEN_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/gui/altar.png");
    private static final ResourceLocation SCREEN_WIDGETS = new ResourceLocation(RPGGods.MODID, "textures/gui/favor_widgets.png");

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
    private static final int RESET_X = 123;
    private static final int RESET_Y = 87;

    private static final int SLIDER_X = 69;
    private static final int SLIDER_Y = 14;
    private static final int SLIDER_HEIGHT = 20;
    private static final int SLIDER_SPACING = 4;

    private static final int BTN_WIDTH = 70;
    private static final int BTN_HEIGHT = 16;

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
    private BlockState block;
    private boolean blockLocked;
    private AltarPose pose;
    private boolean poseLocked;

    protected AngleSlider sliderAngleX;
    protected AngleSlider sliderAngleY;
    protected AngleSlider sliderAngleZ;
    private final PartButton[] partButtons = new PartButton[ModelPart.values().length];
    private IconButton genderButton;
    private IconButton slimButton;
    private IconButton resetButton;
    private ModelPart selectedPart = ModelPart.BODY;

    private TextFieldWidget nameField;

    public AltarScreen(final AltarContainer screenContainer, final PlayerInventory inv, final ITextComponent title) {
        super(screenContainer, inv, title);
        this.xSize = SCREEN_WIDTH;
        this.ySize = SCREEN_HEIGHT - TAB_HEIGHT / 2;
        this.playerInventoryTitleX = this.guiLeft + AltarContainer.PLAYER_INV_X;
        this.playerInventoryTitleY = this.guiTop + AltarContainer.PLAYER_INV_Y - 10;
        Altar altar = screenContainer.getAltar();
        enabled = altar.isEnabled();
        name = altar.getDeity().isPresent() ? Optional.empty() : altar.getName();
        female = altar.isFemale();
        slim = altar.isSlim();
        items = altar.getItems();
        block = altar.getBlock();
        blockLocked = altar.isBlockLocked();
        pose = altar.getPose();
        poseLocked = altar.isPoseLocked();
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        // add tab buttons
        tabButtons[0] = addButton(new AltarScreen.TabButton(this, 0, new TranslationTextComponent("gui.altar.pose"),
                guiLeft + (0 * TAB_WIDTH), guiTop - TAB_HEIGHT + 4, new ItemStack(Items.ARMOR_STAND)));
        tabButtons[1] = addButton(new AltarScreen.TabButton(this, 1, new TranslationTextComponent("gui.altar.items"),
                guiLeft + (1 * TAB_WIDTH), guiTop - TAB_HEIGHT + 4, new ItemStack(Items.IRON_SWORD)));
        // add part buttons
        for (int i = 0, l = ModelPart.values().length; i < l; i++) {
            final ModelPart p = ModelPart.values()[i];
            final ITextComponent title = new TranslationTextComponent("gui.altar." + p.getString());
            partButtons[i] = this.addButton(new PartButton(this, this.guiLeft + PARTS_X, this.guiTop + PARTS_Y + (BTN_HEIGHT * i), title, button -> {
                this.selectedPart = p;
                AltarScreen.this.updateSliders();
            }) {
                @Override
                protected boolean isSelected() {
                    return this.isHovered() || (p == AltarScreen.this.selectedPart);
                }
            });
        }
        // add reset button
        final ITextComponent titleReset = new TranslationTextComponent("controls.reset");
        resetButton = this.addButton(new IconButton(this, this.guiLeft + RESET_X, this.guiTop + RESET_Y, 0, 202, titleReset, button -> {
            AltarScreen.this.pose.set(AltarScreen.this.selectedPart, 0, 0, 0);
            AltarScreen.this.updateSliders();
        }));
        // add gender button
        final ITextComponent titleGender = new TranslationTextComponent("gui.altar.gender");
        genderButton = this.addButton(new IconButton(this, this.guiLeft + GENDER_X, this.guiTop + GENDER_Y, 0, 218, titleGender, button -> AltarScreen.this.female = !AltarScreen.this.female) {
            @Override
            public int getIconX() {
                return super.getIconX() + (AltarScreen.this.female ? 0 : this.width);
            }
        });
        // add slim button
        final ITextComponent titleSlim = new TranslationTextComponent("gui.altar.slim");
        slimButton = this.addButton(new IconButton(this, this.guiLeft + SLIM_X, this.guiTop + SLIM_Y, 32, 218, titleSlim, button -> AltarScreen.this.slim = !AltarScreen.this.slim) {
            @Override
            public int getIconX() {
                return super.getIconX() + (AltarScreen.this.slim ? 0 : this.width);
            }
        });
        // add sliders
        this.sliderAngleX = (new AltarScreen.AngleSlider(this.guiLeft + SLIDER_X, this.guiTop + SLIDER_Y, "X") {
            @Override
            void setAngleValue(double angRadians) { AltarScreen.this.pose.get(AltarScreen.this.selectedPart).setX((float)angRadians); }
            @Override
            double getAngleValue() { return Math.toDegrees(AltarScreen.this.pose.get(AltarScreen.this.selectedPart).getX()); }
        });
        this.sliderAngleY = (new AltarScreen.AngleSlider(this.guiLeft + SLIDER_X, this.guiTop + SLIDER_Y + (SLIDER_HEIGHT + SLIDER_SPACING), "Y") {
            @Override
            void setAngleValue(double angRadians) { AltarScreen.this.pose.get(AltarScreen.this.selectedPart).setY((float)angRadians); }
            @Override
            double getAngleValue() { return Math.toDegrees(AltarScreen.this.pose.get(AltarScreen.this.selectedPart).getY()); }
        });
        this.sliderAngleZ = (new AltarScreen.AngleSlider(this.guiLeft + SLIDER_X, this.guiTop + SLIDER_Y + 2 * (SLIDER_HEIGHT + SLIDER_SPACING), "Z") {
            @Override
            void setAngleValue(double angRadians) { AltarScreen.this.pose.get(AltarScreen.this.selectedPart).setZ((float)angRadians); }
            @Override
            double getAngleValue() { return Math.toDegrees(AltarScreen.this.pose.get(AltarScreen.this.selectedPart).getZ()); }
        });
        this.addButton(sliderAngleX);
        this.addButton(sliderAngleY);
        this.addButton(sliderAngleZ);
        // items tab
        this.minecraft.keyboardListener.enableRepeatEvents(true);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.nameField = new TextFieldWidget(this.font, this.guiLeft + TEXT_X, this.guiTop + TEXT_Y, TEXT_WIDTH, TEXT_HEIGHT, new TranslationTextComponent("gui.altar.name"));
        this.nameField.setText(name.orElse(""));
        this.nameField.setCanLoseFocus(true);
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setEnableBackgroundDrawing(true);
        this.nameField.setMaxStringLength(35);
        this.nameField.setResponder(s -> name = (s != null && s.length() > 0) ? Optional.of(s) : Optional.empty());
        // TODO disable when deity is present
        //this.nameField.setEnabled(!getContainer().getAltar().getDeity().isPresent());
        this.children.add(this.nameField);
        // update
        this.updateSliders();
        this.updateTab(0);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        this.renderBackground(matrixStack);
        RenderHelper.setupGuiFlatDiffuseLighting();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // draw background
        this.minecraft.getTextureManager().bindTexture(SCREEN_TEXTURE);
        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        // draw preview pane
        this.minecraft.getTextureManager().bindTexture(SCREEN_WIDGETS);
        this.blit(matrixStack, this.guiLeft + PREVIEW_X, this.guiTop + PREVIEW_Y, 168, 130, PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw tile entity preview
        drawEntityOnScreen(matrixStack, this.guiLeft + PREVIEW_X + 12, this.guiTop + PREVIEW_Y + 4, mouseX, mouseY, partialTicks);
        // draw text box
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw hovering text LAST
        for (final Widget b : this.buttons) {
            if (b.visible && b.isHovered()) {
                b.renderToolTip(matrixStack, mouseX, mouseY);
            }
        }
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void tick() {
        super.tick();
        this.nameField.tick();
    }

    @Override
    public void onClose() {
        super.onClose();
        this.minecraft.keyboardListener.enableRepeatEvents(false);
        // send update packet to server
        RPGGods.CHANNEL.sendToServer(new CUpdateAltarPacket(this.container.getEntity().getEntityId(), this.pose, this.female, this.slim, this.name.orElse("")));
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.nameField.getText();
        this.init(minecraft, width, height);
        this.nameField.setText(s);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeScreen();
        }

        return !this.nameField.keyPressed(keyCode, scanCode, modifiers) && !this.nameField.canWrite() ? super.keyPressed(keyCode, scanCode, modifiers) : true;
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
        sliderAngleX.visible = tab0;
        sliderAngleY.visible = tab0;
        sliderAngleZ.visible = tab0;
        genderButton.visible = tab0;
        slimButton.visible = tab0;
        resetButton.visible = tab0;
        for(PartButton pb : partButtons) {
            pb.visible = tab0;
        }
        nameField.visible = tab1;
        this.setListener(tab1 ? this.nameField : null);
    }

    @SuppressWarnings("deprecation")
    public void drawEntityOnScreen(final MatrixStack matrixStackIn, final int posX, final int posY,
                                       final float mouseX, final float mouseY, final float partialTicks) {
        float margin = 12;
        float scale = PREVIEW_WIDTH - margin * 2;
        float rotX = (float) Math.atan((double) ((mouseX - this.guiLeft) / 40.0F));
        float rotY = (float) Math.atan((double) ((mouseY - this.guiTop - PREVIEW_HEIGHT / 2) / 40.0F));
        // preview client-side tile entity information
        updateAltarEntity(container.getEntity());

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
        Minecraft.getInstance().getRenderManager().getRenderer(container.getEntity())
                        .render(container.getEntity(), 0F, partialTicks, matrixStackIn, bufferType, 15728880);
        bufferType.finish();

        RenderSystem.enableDepthTest();
        RenderHelper.setupGui3DDiffuseLighting();
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
        entity.setAltarPose(this.pose);
        entity.setFemale(female);
        entity.setSlim(slim);
        if(name.isPresent()) {
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
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                int selected = isSelected() ? 0 : 2;
                final int xOffset = (index % TAB_COUNT) * TAB_WIDTH;
                final int yOffset = isSelected() ? this.height : 2;
                // draw button background
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                AltarScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y - selected, xOffset, yOffset - selected, this.width, this.height - selected);
                // draw item
                AltarScreen.this.itemRenderer.renderItemIntoGUI(item, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
            }
        }

        public boolean isSelected() {
            return AltarScreen.this.tabIndex == index;
        }
    }


    protected class PartButton extends Button {

        public PartButton(final AltarScreen screenIn, final int x, final int y, final ITextComponent title, final IPressable pressedAction) {
            super(x, y, BTN_WIDTH, BTN_HEIGHT, title, pressedAction);
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                final boolean selected = isSelected();
                final int xOffset = 25;
                final int yOffset = 130 + (selected ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                AltarScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
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
            super(x, y, BTN_HEIGHT, BTN_HEIGHT, StringTextComponent.EMPTY, pressedAction,
                    (b, m, bx, by) -> screenIn.renderTooltip(m, screenIn.minecraft.fontRenderer.trimStringToWidth(title, Math.max(screenIn.width / 2 - 43, 170)), bx, by));
            this.textureX = tX;
            this.textureY = tY;
        }

        @Override
        public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                int xOffset = 97;
                int yOffset = 130 + (this.isHovered() ? this.height : 0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                // draw button background
                AltarScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
                // draw button icon
                AltarScreen.this.getMinecraft().getTextureManager().bindTexture(SCREEN_TEXTURE);
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
            super(x, y, BTN_WIDTH, SLIDER_HEIGHT, StringTextComponent.EMPTY, 0.5D);
            rotationName = rName;
            this.func_230979_b_();
        }

        // called when the value is changed
        protected void func_230979_b_() {
            this.setMessage(new TranslationTextComponent("gui.altar.rotation", rotationName, Math.round(getAngleValue())));
        }

        // called when the value is changed and is different from its previous value
        protected void func_230972_a_() {
            setAngleValue(Math.toRadians((this.sliderValue - 0.5D) * getAngleBounds()));
        }

        protected double getValueRadians() {
            return Math.toRadians((this.sliderValue - 0.5D) * getAngleBounds());
        }

        public void updateSlider() {
            this.sliderValue = MathHelper.clamp((getAngleValue() / getAngleBounds()) + 0.5D, 0.0D, 1.0D);
            this.func_230979_b_();
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
