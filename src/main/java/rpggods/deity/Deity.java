package rpggods.deity;

import net.minecraft.util.ResourceLocation;
import rpggods.favor.FavorRange;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkData;

import java.util.ArrayList;
import java.util.EnumMap;
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
    /** Map of PerkCondition.Type to Perk(s). May contain multiple instances of the same Perk. **/
    public final Map<PerkCondition.Type, List<Perk>> perkByConditionMap = new EnumMap<>(PerkCondition.Type.class);
    /** Map of PerkData.Type to Perk(s) **/
    public final Map<PerkData.Type, List<Perk>> perkByTypeMap = new EnumMap<>(PerkData.Type.class);
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
        perkByConditionMap.clear();
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
        if(offering.getFavor() == 0 && !offering.getFunction().isPresent()) {
            return;
        }
        ResourceLocation id = offering.getAccept().getItem().getRegistryName();
        offeringMap.computeIfAbsent(id, r -> new ArrayList<>()).add(offering);
    }

    /**
     * Add a Sacrifice to this deity
     * @param sacrifice the Sacrifice to add
     */
    public void add(final Sacrifice sacrifice) {
        if(sacrifice.getFavor() == 0 && !sacrifice.getFunction().isPresent()) {
            return;
        }
        ResourceLocation id = sacrifice.getEntity();
        sacrificeMap.computeIfAbsent(id, r -> new ArrayList<>()).add(sacrifice);
    }

    /**
     * Adds a Perk to this deity
     * @param perk the Perk to add
     */
    public void add(final Perk perk) {
        if(FavorRange.EMPTY.equals(perk.getRange()) || perk.getActions().isEmpty()) {
            return;
        }
        // add to list
        perkList.add(perk);
        // add to perkByCondition map
        for(PerkCondition condition : perk.getConditions()) {
            perkByConditionMap.computeIfAbsent(condition.getType(), r -> new ArrayList<>()).add(perk);
        }
        // add to perkByType map
        for(final PerkData action : perk.getActions()) {
            PerkData.Type type = action.getType();
            perkByTypeMap.computeIfAbsent(type, r -> new ArrayList<>()).add(perk);
        }
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
