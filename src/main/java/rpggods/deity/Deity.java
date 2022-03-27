package rpggods.deity;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import rpggods.favor.FavorRange;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkAction;

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
    public static final Deity EMPTY = new Deity(new ResourceLocation("null"));

    /** The ResourceLocation ID **/
    public final ResourceLocation id;
    /** List of Altars **/
    public final List<ResourceLocation> altarList = new ArrayList<>();
    /** Map of Item ID to Offering(s) **/
    public final Map<ResourceLocation, List<ResourceLocation>> offeringMap = new HashMap<>();
    /** Map of Entity ID to Sacrifice(s) **/
    public final Map<ResourceLocation, List<ResourceLocation>> sacrificeMap = new HashMap<>();
    /** Map of PerkCondition.Type to Perk(s). May contain multiple instances of the same Perk. **/
    public final Map<PerkCondition.Type, List<ResourceLocation>> perkByConditionMap = new EnumMap<>(PerkCondition.Type.class);
    /** Map of PerkData.Type to Perk(s) **/
    public final Map<PerkAction.Type, List<ResourceLocation>> perkByTypeMap = new EnumMap<>(PerkAction.Type.class);
    /** List of all Perks **/
    public final List<ResourceLocation> perkList = new ArrayList<>();

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

    public void add(final ResourceLocation id, final Altar altar) {
        altarList.add(id);
    }

    /**
     * Add an Offering to this deity
     * @param id the offering ID
     * @param offering the Offering to add
     */
    public void add(final ResourceLocation id, final Offering offering) {
        if(offering.getFavor() == 0 && !offering.getFunction().isPresent() && !offering.getTrade().isPresent()) {
            return;
        }
        ResourceLocation itemId = offering.getAccept().getItem().getRegistryName();
        offeringMap.computeIfAbsent(itemId, r -> new ArrayList<>()).add(id);
    }

    /**
     * Add a Sacrifice to this deity
     * @param id the sacrifice ID
     * @param sacrifice the Sacrifice to add
     */
    public void add(final ResourceLocation id, final Sacrifice sacrifice) {
        if(sacrifice.getFavor() == 0 && !sacrifice.getFunction().isPresent()) {
            return;
        }
        ResourceLocation entityId = sacrifice.getEntity();
        sacrificeMap.computeIfAbsent(entityId, r -> new ArrayList<>()).add(id);
    }

    /**
     * Adds a Perk to this deity
     * @param id the Perk ID
     * @param perk the Perk to add
     */
    public void add(final ResourceLocation id, final Perk perk) {
        if(FavorRange.EMPTY.equals(perk.getRange()) || perk.getActions().isEmpty()) {
            return;
        }
        // add to list
        perkList.add(id);
        // add to perkByCondition map
        for(PerkCondition condition : perk.getConditions()) {
            perkByConditionMap.computeIfAbsent(condition.getType(), r -> new ArrayList<>()).add(id);
        }
        // add to perkByType map
        for(final PerkAction action : perk.getActions()) {
            PerkAction.Type type = action.getType();
            perkByTypeMap.computeIfAbsent(type, r -> new ArrayList<>()).add(id);
        }
    }

    public static ITextComponent getName(final ResourceLocation id) {
        return new TranslationTextComponent(Altar.createTranslationKey(id));
    }

    @Override
    public String toString() {
        int offerings = 0;
        for(List<ResourceLocation> o : offeringMap.values()) {
            offerings += o.size();
        }
        int sacrifices = 0;
        for(List<ResourceLocation> s : sacrificeMap.values()) {
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
