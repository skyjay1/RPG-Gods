package rpggods.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.entity.AltarEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class AltarStructureProcessor extends StructureProcessor {

    public static final AltarStructureProcessor PROCESSOR = new AltarStructureProcessor();
    public static final Codec<AltarStructureProcessor> CODEC = Codec.unit(() -> PROCESSOR);

    public AltarStructureProcessor() {
        super();
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return RGRegistry.ALTAR_STRUCTURE_PROCESSOR;
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos seedPos,
                                                               StructureTemplate.StructureEntityInfo rawEntityInfo,
                                                               StructureTemplate.StructureEntityInfo entityInfo,
                                                               StructurePlaceSettings placementSettings,
                                                               StructureTemplate template) {
        // read entity type from tag
        CompoundTag tag = entityInfo.nbt.copy();
        Optional<EntityType<?>> entityType = EntityType.by(tag);
        // ensure entity is altar
        if (entityType.isPresent() && entityType.get() == RGRegistry.ALTAR_TYPE.get()) {
            // determine random instance
            final long seed = placementSettings.getBoundingBox().hashCode();
            Random random = new Random(seed);
            // choose random deity
            ResourceLocation altarId = new ResourceLocation("empty");
            List<ResourceLocation> altarIds = new ArrayList<>(RPGGods.ALTAR_MAP.keySet());
            if (!altarIds.isEmpty()) {
                // sort list, then choose random element
                altarIds.sort(ResourceLocation::compareNamespaced);
                altarId = altarIds.get(random.nextInt(altarIds.size()));
            }
            // write deity to altar
            CompoundTag modified = AltarEntity.writeAltarProperties(altarId, placementSettings.getRotation());
            tag.merge(modified);
            // create modified entity info
            return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, tag);
        }
        return entityInfo;
    }
}
