package rpggods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;

public class RGSavedData extends SavedData {

    private static final String KEY_FAVOR = "favor";

    private final IFavor favor;

    public RGSavedData() {
        favor = new Favor();
    }

    public static RGSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(RGSavedData::read, RGSavedData::new, RPGGods.MODID);
    }

    public static RGSavedData read(CompoundTag nbt) {
        RGSavedData instance = new RGSavedData();
        instance.load(nbt);
        return instance;
    }

    public void load(CompoundTag tag) {
        if(tag.contains(KEY_FAVOR)) {
            this.favor.deserializeNBT(tag.getCompound(KEY_FAVOR));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(KEY_FAVOR, this.favor.serializeNBT());
        return tag;
    }

    public IFavor getFavor() {
        return favor;
    }
}
