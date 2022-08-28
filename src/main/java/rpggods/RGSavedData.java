package rpggods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RGSavedData extends SavedData {

    private static final String KEY_FAVOR = "favor";
    private static final String KEY_TEAMS = "teams";
    private static final String KEY_NAME = "name";

    /** Favor instance for global favor **/
    private final IFavor favor;
    /** Favor instances for each team where Key=(Team name); Value=(Favor) **/
    private final Map<String, IFavor> teamFavor;

    public RGSavedData() {
        favor = new Favor();
        teamFavor = new HashMap<>();
    }

    /**
     * Gets or creates a saved data for the given server.
     * @param server the minecraft server
     * @return the saved data for the server, will be the same for any level
     */
    public static RGSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(RGSavedData::read, RGSavedData::new, RPGGods.MODID);
    }

    /**
     * Creates and loads a saved data instance
     * @param nbt the saved data compound tag NBT
     * @return the loaded saved data
     */
    public static RGSavedData read(CompoundTag nbt) {
        RGSavedData instance = new RGSavedData();
        instance.load(nbt);
        return instance;
    }

    /**
     * Loads this saved data from the compound tag NBT
     * @param tag the saved data NBT
     */
    public void load(CompoundTag tag) {
        teamFavor.clear();
        // load global favor
        if(tag.contains(KEY_FAVOR)) {
            this.favor.deserializeNBT(tag.getCompound(KEY_FAVOR));
        }
        // load team favor
        if(tag.contains(KEY_TEAMS)) {
            ListTag listTag = tag.getList(KEY_TEAMS, Tag.TAG_COMPOUND);
            CompoundTag temp;
            String team;
            CompoundTag favor;
            for(int i = 0, n = listTag.size(); i < n; i++) {
                temp = listTag.getCompound(i);
                team = temp.getString(KEY_NAME);
                favor = temp.getCompound(KEY_FAVOR);
                Favor f = new Favor();
                f.deserializeNBT(favor);
                teamFavor.put(team, f);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(KEY_FAVOR, this.favor.serializeNBT());
        ListTag listTag = new ListTag();
        for(Map.Entry<String, IFavor> entry : teamFavor.entrySet()) {
            CompoundTag temp = new CompoundTag();
            temp.putString(KEY_NAME, entry.getKey());
            temp.put(KEY_FAVOR, entry.getValue().serializeNBT());
            listTag.add(temp);
        }
        tag.put(KEY_TEAMS, listTag);
        return tag;
    }

    /**
     * Marks the server data as dirty, then returns the global favor.
     * @return the server global favor
     */
    public IFavor getFavor() {
        this.setDirty();
        return favor;
    }

    /**
     * @return the saved team names
     */
    public Collection<String> getTeams() {
        return teamFavor.keySet();
    }

    /**
     * @return the favor instances for each team
     */
    public Collection<IFavor> getTeamFavor() {
        return teamFavor.values();
    }

    /**
     * Marks the saved data as dirty, then gets or creates a favor instance for the given team
     * @param team the team name
     * @return the favor instance for the team
     */
    public IFavor getTeamFavor(String team) {
        this.setDirty();
        if(!teamFavor.containsKey(team)) {
            teamFavor.put(team, new Favor());
        }
        return teamFavor.get(team);
    }

    /**
     * Unused.
     * Removes a favor instance for the given team
     * @param team the team name
     * @return true if the favor was removed
     */
    public boolean removeTeamFavor(String team) {
        if(teamFavor.remove(team) != null) {
            this.setDirty();
            return true;
        }
        return false;
    }
}
