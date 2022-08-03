package rpggods.deity;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class Cooldown implements INBTSerializable<CompoundTag> {

    public static Cooldown EMPTY = new Cooldown(1, 0);

    private static final String MAX_USES = "maxuses";
    private static final String MAX_COOLDOWN = "maxcooldown";
    private static final String MAX_RESTOCKS = "maxrestocks";
    private static final String RESTOCKS = "restocks";
    private static final String COOLDOWN = "cooldown";
    private static final String USES = "uses";

    private int maxUses;
    private int uses;
    private int maxRestocks;
    private int restocks;
    private long maxCooldown;
    private long cooldown;

    public Cooldown(CompoundTag nbt) {
        deserializeNBT(nbt);
    }

    public Cooldown(int maxUses, long maxCooldown) {
        this(maxUses, maxCooldown, -1);
    }

    public Cooldown(int maxUses, long maxCooldown, int maxRestocks) {
        this.maxUses = maxUses;
        this.maxCooldown = maxCooldown;
        this.maxRestocks = maxRestocks;
        this.cooldown = 0;
        this.uses = 0;
        this.restocks = 0;
    }

    /** @return the maximum number of uses until cooldown is triggered **/
    public int getMaxUses() {
        return maxUses;
    }

    /** @return the maximum number of restocks **/
    public int getMaxRestocks() {
        return maxRestocks;
    }

    /** @return the cooldown amount (consider un-usable if cooldown is above zero) **/
    public long getCooldown() {
        return cooldown;
    }

    /** @param add the amount of cooldown to add or remove **/
    public void addCooldown(long add) {
        this.cooldown += add;
    }

    /** Sets cooldown to the max **/
    public void applyCooldown() {
        cooldown = maxCooldown;
    }

    /**
     * Adds to the amount of uses and applies cooldown and restocks if needed
     * @return true if there are any uses remaining
     */
    public boolean addUse() {
        if(++uses >= maxUses) {
            applyCooldown();
            restock();
            return false;
        }
        return true;
    }

    /** Sets uses to 0 **/
    public void resetUses() {
        uses = 0;
    }

    /** Resets uses and increases restock count **/
    public void restock() {
        resetUses();
        restocks++;
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
        return uses < maxUses && cooldown <= 0 && (maxRestocks < 0 || restocks < maxRestocks);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Cooldown: [");
        sb.append(" uses=").append(uses);
        sb.append(" maxuses=").append(maxUses);
        sb.append(" restocks=").append(restocks);
        sb.append(" maxrestocks=").append(maxRestocks);
        sb.append(" cooldown=").append(cooldown);
        sb.append(" maxcooldown=").append(maxCooldown);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(MAX_USES, maxUses);
        tag.putLong(MAX_COOLDOWN, maxCooldown);
        tag.putLong(COOLDOWN, cooldown);
        tag.putInt(USES, uses);
        tag.putInt(RESTOCKS, restocks);
        tag.putInt(MAX_RESTOCKS, maxRestocks);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.maxUses = nbt.getInt(MAX_USES);
        this.maxCooldown = nbt.getLong(MAX_COOLDOWN);
        this.cooldown = nbt.getLong(COOLDOWN);
        this.uses = nbt.getInt(USES);
        this.restocks = nbt.getInt(RESTOCKS);
        this.maxRestocks = nbt.getInt(MAX_RESTOCKS);
    }
}
