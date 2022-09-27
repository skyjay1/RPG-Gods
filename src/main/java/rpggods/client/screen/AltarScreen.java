package rpggods.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import rpggods.RPGGods;
import rpggods.util.altar.AltarPose;
import rpggods.util.altar.HumanoidPart;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;
import rpggods.menu.AltarContainerMenu;
import rpggods.network.CUpdateAltarPacket;

import java.util.Optional;

public class AltarScreen extends AbstractContainerScreen<AltarContainerMenu> {

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

    private Optional<String> name;
    private boolean female;
    private boolean slim;
    private AltarPose pose;
    private boolean poseLocked;

    protected AngleSlider sliderAngleX;
    protected AngleSlider sliderAngleY;
    protected AngleSlider sliderAngleZ;
    private final PartButton[] partButtons = new PartButton[HumanoidPart.values().length];
    private IconButton genderButton;
    private IconButton slimButton;
    private IconButton presetButton;
    private IconButton resetButton;
    private HumanoidPart selectedPart = HumanoidPart.BODY;

    private EditBox nameField;

    public AltarScreen(final AltarContainerMenu screenContainer, final Inventory inv, final Component title) {
        super(screenContainer, inv, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT - TAB_HEIGHT / 2;
        this.inventoryLabelX = this.leftPos + AltarContainerMenu.PLAYER_INV_X;
        this.inventoryLabelY = this.topPos + AltarContainerMenu.PLAYER_INV_Y - 10;
        Altar altar = screenContainer.getAltar();
        if(altar.getDeity().isPresent()) {
            name = Optional.empty();
        } else if(screenContainer.getEntity().hasCustomName()) {
            name = Optional.of(screenContainer.getEntity().getCustomName().getString());
        } else {
            name = Optional.empty();
        }
        female = altar.isFemale();
        slim = altar.isSlim();
        pose = altar.getPose();
        poseLocked = altar.isPoseLocked();
    }

    @Override
    public void init() {
        super.init();
        // add tab buttons
        tabButtons[0] = addRenderableWidget(new AltarScreen.TabButton(this, 0, Component.translatable("gui.altar.pose"),
                leftPos + (0 * TAB_WIDTH), topPos - TAB_HEIGHT + 4, new ItemStack(Items.ARMOR_STAND)));
        tabButtons[1] = addRenderableWidget(new AltarScreen.TabButton(this, 1, Component.translatable("gui.altar.items"),
                leftPos + (1 * TAB_WIDTH), topPos - TAB_HEIGHT + 4, new ItemStack(Items.IRON_SWORD)));
        // add part buttons
        for (int i = 0, l = HumanoidPart.values().length; i < l; i++) {
            final HumanoidPart p = HumanoidPart.values()[i];
            final Component title = Component.translatable("gui.altar." + p.getSerializedName());
            partButtons[i] = this.addRenderableWidget(new PartButton(this, this.leftPos + PARTS_X, this.topPos + PARTS_Y + (PART_HEIGHT * i), title, button -> {
                this.selectedPart = p;
                AltarScreen.this.updateSliders();
            }) {
                @Override
                public boolean isHoveredOrFocused() {
                    return super.isHoveredOrFocused() || (p == AltarScreen.this.selectedPart);
                }
            });
        }
        // add randomize button
        final Component titlePreset = Component.translatable("gui.altar.preset");
        final AltarPose[] presets = new AltarPose[] { AltarPose.STANDING_HOLDING, AltarPose.STANDING_RAISED,
                AltarPose.STANDING_HOLDING_DRAMATIC, AltarPose.WALKING, AltarPose.WEEPING, AltarPose.DAB };
        presetButton = this.addRenderableWidget(new IconButton(this, this.leftPos + PRESET_X, this.topPos + PRESET_Y, 16, 202, titlePreset, button -> {
            this.pose = presets[(int)Math.floor(Math.random() * presets.length)];
        }));
        // add reset button
        final Component titleReset = Component.translatable("controls.reset");
        resetButton = this.addRenderableWidget(new IconButton(this, this.leftPos + RESET_X, this.topPos + RESET_Y, 0, 202, titleReset, button -> {
            AltarScreen.this.pose.set(AltarScreen.this.selectedPart, 0, 0, 0);
            AltarScreen.this.updateSliders();
        }));
        // add gender button
        final Component titleGender = Component.translatable("gui.altar.gender");
        genderButton = this.addRenderableWidget(new IconButton(this, this.leftPos + GENDER_X, this.topPos + GENDER_Y, 0, 218, titleGender, button -> AltarScreen.this.female = !AltarScreen.this.female) {
            @Override
            public int getIconX() {
                return super.getIconX() + (AltarScreen.this.female ? 0 : this.width);
            }
        });
        // add slim button
        final Component titleSlim = Component.translatable("gui.altar.slim");
        slimButton = this.addRenderableWidget(new IconButton(this, this.leftPos + SLIM_X, this.topPos + SLIM_Y, 32, 218, titleSlim, button -> AltarScreen.this.slim = !AltarScreen.this.slim) {
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
        this.addRenderableWidget(sliderAngleX);
        this.addRenderableWidget(sliderAngleY);
        this.addRenderableWidget(sliderAngleZ);
        // items tab
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.nameField = new EditBox(this.font, this.leftPos + TEXT_X, this.topPos + TEXT_Y, TEXT_WIDTH, TEXT_HEIGHT, Component.translatable("gui.altar.name"));
        this.nameField.setValue(name.orElse(""));
        this.nameField.setCanLoseFocus(true);
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setBordered(true);
        this.nameField.setMaxLength(35);
        this.nameField.setResponder(s -> name = (s != null && s.length() > 0) ? Optional.of(s) : Optional.empty());
        this.nameField.setEditable(!getMenu().getEntity().isNameLocked());
        this.addRenderableWidget(this.nameField);
        // update
        this.updateSliders();
        this.updateTab(0);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        this.renderBackground(matrixStack);
        Lighting.setupForFlatItems();
        // draw background
        RenderSystem.setShaderTexture(0, SCREEN_TEXTURE);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        // draw item slots
        if(this.tabIndex == 1) {
            // slots on texture are at 31, 119
            for(int i = 0, l = getMenu().getAltarSlots().size(); i < l; i++) {
                Slot slot = getMenu().getAltarSlots().get(i);
                this.blit(matrixStack, this.leftPos + slot.x - 1, this.topPos + slot.y - 1,
                        AltarContainerMenu.PLAYER_INV_X - 1, AltarContainerMenu.PLAYER_INV_Y - 1, 18, 18);
                // render slab icon on last slot
                if(i == l - 1 && slot.getItem().isEmpty()) {
                    this.blit(matrixStack, this.leftPos + slot.x, this.topPos + slot.y,
                            48, 202, 16, 16);
                }
            }
        }
        // draw preview pane
        RenderSystem.setShaderTexture(0, SCREEN_WIDGETS);
        this.blit(matrixStack, this.leftPos + PREVIEW_X, this.topPos + PREVIEW_Y, 168, 130, PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw entity preview
        drawEntityOnScreen(matrixStack, this.leftPos + PREVIEW_X + PREVIEW_WIDTH / 2, this.topPos + PREVIEW_Y + PREVIEW_HEIGHT, mouseX, mouseY, partialTicks);
        // draw text box
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
        // draw hovering text LAST
        for(GuiEventListener w : this.children()) {
            if(w instanceof Button b && b.visible && b.isHoveredOrFocused()) {
                matrixStack.pushPose();
                b.renderToolTip(matrixStack, mouseX, mouseY);
                matrixStack.popPose();
            }
        }
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
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
            if(slot instanceof AltarContainerMenu.AltarSlot) {
                ((AltarContainerMenu.AltarSlot)slot).setHidden(tab0);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void drawEntityOnScreen(final PoseStack matrixStackIn, final int posX, final int posY,
                                       final float mouseX, final float mouseY, final float partialTicks) {
        float margin = 12;
        float scale = PREVIEW_WIDTH - margin * 2;
        float rotX = (float) Math.atan((double) ((mouseX - this.leftPos) / 40.0F));
        float rotY = (float) Math.atan((double) ((mouseY - this.topPos - PREVIEW_HEIGHT / 2) / 40.0F));
        // preview client-side tile entity information
        updateAltarEntity(menu.getEntity());

        // Render the Entity with given scale
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate((double)posX, (double)posY, 1050.0D);
        posestack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack posestack1 = new PoseStack();
        posestack1.translate(0.0D, 0.0D, 1000.0D);
        posestack1.scale(scale, scale, scale);
        Quaternion quaternion = Vector3f.YP.rotationDegrees(rotX * -15.0F + 180.0F - menu.getEntity().getYRot()); // was 180.0F
        Quaternion quaternion1 = Vector3f.XP.rotationDegrees(rotY * -15.0F);
        Quaternion quaternion2 = Vector3f.ZP.rotationDegrees(180.0F);
        quaternion.mul(quaternion1);
        quaternion.mul(quaternion2);
        posestack1.mulPose(quaternion);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conj();
        entityrenderdispatcher.overrideCameraOrientation(quaternion1);
        entityrenderdispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(menu.getEntity(), 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, posestack1, multibuffersource$buffersource, 15728880);
        });
        multibuffersource$buffersource.endBatch();
        entityrenderdispatcher.setRenderShadow(true);
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
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
            entity.setCustomName(Component.literal(name.get()));
        }
    }

    protected class TabButton extends Button {

        private int index;
        private ItemStack item = ItemStack.EMPTY;

        public TabButton(final AltarScreen screenIn, final int index, final Component title, final int x, final int y, ItemStack item) {
            super(x, y, TAB_WIDTH, TAB_HEIGHT, title, b -> screenIn.updateTab(index),
                    (b, m, bx, by) -> screenIn.renderTooltip(m, b.getMessage(), bx, by));
            this.index = index;
            this.item = item;
            this.setMessage(title);
        }

        @Override
        public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if(this.visible) {
                int selected = isSelected() ? 0 : 2;
                final int xOffset = (index % TAB_COUNT) * TAB_WIDTH;
                final int yOffset = isSelected() ? this.height : 2;
                // draw button background
                RenderSystem.setShaderTexture(0, SCREEN_WIDGETS);
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

        public PartButton(final AltarScreen screenIn, final int x, final int y, final Component title, final OnPress pressedAction) {
            super(x, y, PART_WIDTH, PART_HEIGHT, title, pressedAction);
        }

        @Override
        public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                final boolean selected = isHoveredOrFocused();
                final int xOffset = 25;
                final int yOffset = 130 + (selected ? this.height : 0);
                RenderSystem.setShaderTexture(0, SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
                drawCenteredString(matrixStack, AltarScreen.this.font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
            }
        }
    }

    protected class IconButton extends Button {

        private final int textureX;
        private final int textureY;

        public IconButton(final AltarScreen screenIn, final int x, final int y, final int tX, final int tY,
                          final Component title, final OnPress pressedAction) {
            super(x, y, ICON_WIDTH, ICON_HEIGHT, Component.empty(), pressedAction,
                    (b, m, bx, by) -> screenIn.renderTooltip(m, screenIn.minecraft.font.split(title, Math.max(screenIn.width / 2 - 43, 170)), bx, by));
            this.textureX = tX;
            this.textureY = tY;
        }

        @Override
        public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                int xOffset = 97;
                int yOffset = 130 + (this.isHoveredOrFocused() ? this.height : 0);
                // draw button background
                RenderSystem.setShaderTexture(0, SCREEN_WIDGETS);
                this.blit(matrixStack, this.x, this.y, xOffset, yOffset, this.width, this.height);
                // draw button icon
                RenderSystem.setShaderTexture(0, SCREEN_TEXTURE);
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

    protected abstract class AngleSlider extends AbstractSliderButton {

        private final String rotationName;

        public AngleSlider(final int x, final int y, final String rName) {
            super(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, Component.empty(), 0.5D);
            rotationName = rName;
            this.updateMessage();
        }

        // called when the value is changed
        protected void updateMessage() {
            this.setMessage(Component.translatable("gui.altar.rotation", rotationName, Math.round(getAngleValue())));
        }

        // called when the value is changed and is different from its previous value
        protected void applyValue() {
            setAngleValue(Math.toRadians((this.value - 0.5D) * getAngleBounds()));
        }

        protected double getValueRadians() {
            return Math.toRadians((this.value - 0.5D) * getAngleBounds());
        }

        public void updateSlider() {
            this.value = Mth.clamp((getAngleValue() / getAngleBounds()) + 0.5D, 0.0D, 1.0D);
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
