package rpggods.deity;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public class Cooldown implements INBTSerializable<CompoundNBT> {

    public static Cooldown EMPTY = new Cooldown(1, 0);

    private static final String MAX_USES = "maxuses";
    private static final String MAX_COOLDOWN = "maxcooldown";
    private static final String COOLDOWN = "cooldown";
    private static final String USES = "uses";

    private int maxUses;
    private int uses;
    private long maxCooldown;
    private long cooldown;

    public Cooldown(CompoundNBT nbt) {
        deserializeNBT(nbt);
    }

    public Cooldown(int maxUses, long maxCooldown) {
        this.maxUses = maxUses;
        this.maxCooldown = maxCooldown;
        this.cooldown = 0;
        this.uses = 0;
    }

    /** @return the maximum number of uses until cooldown is triggered **/
    public int getMaxUses() {
        return maxUses;
    }

    /** @return the cooldown amount (consider un-usable if cooldown is above zero) **/
    public long getCooldown() {
        return cooldown;
    }

    /** Adds or removes cooldown **/
    public void addCooldown(long add) {
        this.cooldown += add;
    }

    /** Sets cooldown to the max and resets uses **/
    public void applyCooldown() {
        cooldown = maxCooldown;
    }

    /**
     * Adds to the amount of uses
     * @return true if there are any uses remaining
     */
    public boolean addUse() {
        // apply cooldown
        if(++uses >= maxUses) {
            applyCooldown();
            resetUses();
            return false;
        }
        return true;
    }

    /** Sets uses to 0 **/
    public void resetUses() {
        uses = 0;
    }

    /** @return the number of uses left before triggering the cooldown **/
    public int getUsesRemaining() {
        return Math.max(0, maxUses - uses);
    }

    /** @return True if the uses remaining is less than the max **/
    public boolean hasUsesRemaining() {
        return getUsesRemaining() > 0;
    }

    /** @return true if there are uses remaining and no cooldown **/
    public boolean canUse() {
        return uses < maxUses && cooldown <= 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Cooldown: [");
        sb.append(" uses=").append(uses);
        sb.append(" maxuses=").append(maxUses);
        sb.append(" cooldown=").append(cooldown);
        sb.append(" maxcooldown=").append(maxCooldown);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt(MAX_USES, maxUses);
        tag.putLong(MAX_COOLDOWN, maxCooldown);
        tag.putLong(COOLDOWN, cooldown);
        tag.putInt(USES, uses);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.maxUses = nbt.getInt(MAX_USES);
        this.maxCooldown = nbt.getLong(MAX_COOLDOWN);
        this.cooldown = nbt.getLong(COOLDOWN);
        this.uses = nbt.getInt(USES);
    }
}
