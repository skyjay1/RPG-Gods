package rpggods.deity;

import net.minecraft.util.ResourceLocation;
import rpggods.perk.Perk;
import rpggods.perk.PerkData;
import rpggods.perk.PerkTrigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralizes all offerings, sacrifices, and perks
 * for quicker and easier lookup
 */
public class Deity {
    /** The ResourceLocation ID **/
    public final ResourceLocation id;
    /** List of Altars **/
    public final List<Altar> altarList = new ArrayList<>();
    /** Map of Item ID to Offering(s) **/
    public final Map<ResourceLocation, List<Offering>> offeringMap = new HashMap<>();
    /** Map of Entity ID to Sacrifice(s) **/
    public final Map<ResourceLocation, List<Sacrifice>> sacrificeMap = new HashMap<>();
    /** Map of PerkTrigger.Type to Perk(s) **/
    public final Map<PerkTrigger.Type, List<Perk>> perkByTriggerMap = new HashMap<>();
    /** Map of PerkData.Type to Perk(s) **/
    public final Map<PerkData.Type, List<Perk>> perkByTypeMap = new HashMap<>();
    /** List of all Perks **/
    public final List<Perk> perkList = new ArrayList<>();

    public Deity(ResourceLocation id) {
        this.id = id;
    }

    /**
     * Removes all Offerings, Sacrifices, and Perks from this deity
     */
    public void clear() {
        altarList.clear();
        offeringMap.clear();
        sacrificeMap.clear();
        perkByTriggerMap.clear();
        perkByTypeMap.clear();
        perkList.clear();
    }

    public void add(final Altar altar) {
        altarList.add(altar);
    }

    /**
     * Add an Offering to this deity
     * @param offering the Offering to add
     */
    public void add(final Offering offering) {
        ResourceLocation id = offering.getAccept().getItem().getRegistryName();
        if(!offeringMap.containsKey(id)) {
            offeringMap.put(id, new ArrayList<>());
        }
        offeringMap.get(id).add(offering);
    }

    /**
     * Add a Sacrifice to this deity
     * @param sacrifice the Sacrifice to add
     */
    public void add(final Sacrifice sacrifice) {
        ResourceLocation id = sacrifice.getEntity();
        if(!sacrificeMap.containsKey(id)) {
            sacrificeMap.put(id, new ArrayList<>());
        }
        sacrificeMap.get(id).add(sacrifice);
    }

    /**
     * Adds a Perk to this deity
     * @param perk the Perk to add
     */
    public void add(final Perk perk) {
        // add to list
        perkList.add(perk);
        // add to perkByTrigger map
        if(perk.getTrigger().isPresent()) {
            PerkTrigger.Type type = perk.getTrigger().get().getType();
            if(!perkByTriggerMap.containsKey(type)) {
                perkByTriggerMap.put(type, new ArrayList<>());
            }
            perkByTriggerMap.get(type).add(perk);
        }
        // add to perkByType map
        PerkData.Type type = perk.getData().getType();
        if(!perkByTypeMap.containsKey(type)) {
            perkByTypeMap.put(type, new ArrayList<>());
        }
        perkByTypeMap.get(type).add(perk);
    }

    @Override
    public String toString() {
        int offerings = 0;
        for(List<Offering> o : offeringMap.values()) {
            offerings += o.size();
        }
        int sacrifices = 0;
        for(List<Sacrifice> s : sacrificeMap.values()) {
            sacrifices += s.size();
        }
        final StringBuilder sb = new StringBuilder("Deity:");
        sb.append(" id[").append(id).append("]");
        sb.append(" altars[").append(altarList.size()).append("]");
        sb.append(" offerings[").append(offerings).append("]");
        sb.append(" sacrifices[").append(sacrifices).append("]");
        sb.append(" perks[").append(perkList.size()).append("]");
        return sb.toString();
    }
}
