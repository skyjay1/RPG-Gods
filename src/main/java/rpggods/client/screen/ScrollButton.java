package rpggods.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ScrollButton<T extends Screen> extends Button {

    /**
     * A consumer to handle when the scroll ends
     **/
    private final Consumer<ScrollButton<T>> scrollEndHandler;
    /**
     * Amount scrolled (0.0 = top, 1.0 = bottom)
     **/
    private float scrollAmount;
    /**
     * If the scroll bar is enabled
     **/
    private final Predicate<T> enabled;

    private final T screen;
    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final boolean vertical;
    private final int uWidth;
    private final int vHeight;

    public ScrollButton(final T gui, final int x, final int y,
                        final int width, final int height, final int uX, final int vY,
                        final ResourceLocation textureIn, final boolean isVertical, final Predicate<T> isEnabled,
                        final Consumer<ScrollButton<T>> onScrollEnd) {
        super(x, y, width, height, StringTextComponent.EMPTY, b -> {
        });
        screen = gui;
        u = uX;
        v = vY;
        texture = textureIn;
        vertical = isVertical;
        if (isVertical) {
            uWidth = 12;
            vHeight = 15;
        } else {
            uWidth = 15;
            vHeight = 12;
        }
        enabled = isEnabled;
        scrollEndHandler = onScrollEnd;
        scrollAmount = 0;
        scrollEndHandler.accept(this);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            screen.getMinecraft().getTextureManager().bind(texture);
            final boolean isEnabled = enabled.test(screen);
            final float scroll = isEnabled ? scrollAmount : 0.0F;
            final int vOffset = isEnabled ? 0 : vHeight;
            final int offset = MathHelper.clamp((int) (scroll * this.height - this.vHeight / 2), 1, this.height - vHeight - 1);
            final int dx = vertical ? 1 : offset;
            final int dy = vertical ? offset : 1;
            this.blit(matrixStack, this.x + dx, this.y + dy, u, v + vOffset, uWidth, vHeight);
        }
    }

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        if (enabled.test(screen)) {
            updateScrollAmount(mouseX, mouseY);
            scrollEndHandler.accept(this);
        }
    }

    @Override
    public void onDrag(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        if (enabled.test(screen)) {
            updateScrollAmount(mouseX, mouseY);
        }
    }

    @Override
    public void onRelease(final double mouseX, final double mouseY) {
        if (enabled.test(screen)) {
            updateScrollAmount(mouseX, mouseY);
            scrollEndHandler.accept(this);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (enabled.test(screen)) {
            scrollAmount = MathHelper.clamp((float) (scrollAmount - delta), 0.0F, 1.0F);
            scrollEndHandler.accept(this);
            return true;
        }
        return false;
    }

    private void updateScrollAmount(final double mouseX, final double mouseY) {
        if(vertical) {
            scrollAmount = MathHelper.clamp((float) (mouseY - this.y) / (float) this.height, 0.0F, 1.0F);
        } else {
            scrollAmount = MathHelper.clamp((float) (mouseX - this.x) / (float) this.width, 0.0F, 1.0F);
        }
    }

    public float getScrollAmount() {
        return scrollAmount;
    }

    public void resetScroll() {
        scrollAmount = 0.0F;
        scrollEndHandler.accept(this);
    }
}