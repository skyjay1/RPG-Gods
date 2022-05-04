package rpggods.altar;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import com.mojang.math.Vector3f;
import net.minecraftforge.common.util.INBTSerializable;
import rpggods.deity.Altar;

import java.util.EnumMap;
import java.util.Map.Entry;

public class AltarPose implements INBTSerializable<CompoundTag> {

  public static final AltarPose EMPTY = new AltarPose();

  public static final AltarPose WALKING = new AltarPose()
          .set(ModelPart.HEAD, 5, 0, 0)
          .set(ModelPart.LEFT_ARM, 30, 0, -2.5F)
          .set(ModelPart.RIGHT_ARM, -30, 0, 2.5F)
          .set(ModelPart.LEFT_LEG, -20, 0, -2)
          .set(ModelPart.RIGHT_LEG, 20, 0, 2);

  public static final AltarPose STANDING_HOLDING = new AltarPose()
          .set(ModelPart.HEAD, 5, 0, 0)
          .set(ModelPart.LEFT_ARM, 0, 0, -2.5F)
          .set(ModelPart.RIGHT_ARM, -30, 0, 2.5F)
          .set(ModelPart.LEFT_LEG, 0, 0, -2)
          .set(ModelPart.RIGHT_LEG, 0, 0, 2);

  public static final AltarPose STANDING_HOLDING_DRAMATIC = new AltarPose()
          .set(ModelPart.HEAD, -5, 0, 0)
          .set(ModelPart.LEFT_ARM, 0, 0, -2.5F)
          .set(ModelPart.RIGHT_ARM, -90, 0, 2.5F)
          .set(ModelPart.LEFT_LEG, 0, 0, -2)
          .set(ModelPart.RIGHT_LEG, 0, 0, 2);

  public static final AltarPose STANDING_RAISED = new AltarPose()
          .set(ModelPart.HEAD, -20, 0, 0)
          .set(ModelPart.LEFT_ARM, 0, -90F, -130F)
          .set(ModelPart.RIGHT_ARM, 0, 90F, 130F)
          .set(ModelPart.LEFT_LEG, 0, 0, -2)
          .set(ModelPart.RIGHT_LEG, 0, 0, 2);

  public static final AltarPose WEEPING = new AltarPose()
          .set(ModelPart.HEAD, 12, 0, 0)
          .set(ModelPart.LEFT_ARM, -125F, 0, -45F)
          .set(ModelPart.RIGHT_ARM, -125F, 0, 45F)
          .set(ModelPart.LEFT_LEG, 4, 0, -2)
          .set(ModelPart.RIGHT_LEG, -4, 0, 2);

  public static final AltarPose DAB = new AltarPose()
          .set(ModelPart.HEAD, 38, 0, 0)
          .set(ModelPart.LEFT_ARM, -100F, 45F, 0)
          .set(ModelPart.RIGHT_ARM, -108F, 64F, 0)
          .set(ModelPart.LEFT_LEG, 0, 0, -4)
          .set(ModelPart.RIGHT_LEG, 0, 0, 4);


  public static final Codec<AltarPose> CODEC = CompoundTag.CODEC.xmap(AltarPose::new, AltarPose::serializeNBT);

  private final EnumMap<ModelPart, Vector3f> angles = new EnumMap<>(ModelPart.class);
  
  public AltarPose() {
    angles.put(ModelPart.HEAD, new Vector3f(0, 0, 0));
    angles.put(ModelPart.BODY, new Vector3f(0, 0, 0));
    angles.put(ModelPart.LEFT_ARM, new Vector3f(0, 0, 0));
    angles.put(ModelPart.RIGHT_ARM, new Vector3f(0, 0, 0));
    angles.put(ModelPart.LEFT_LEG, new Vector3f(0, 0, 0));
    angles.put(ModelPart.RIGHT_LEG, new Vector3f(0, 0, 0));
    angles.put(ModelPart.OFFSET, new Vector3f(0, 0, 0));
  }
  
  public AltarPose(final CompoundTag tag) {
    deserializeNBT(tag);
  }
  
  /**
   * Adds a model rotation to the AltarPose using degrees
   * @param p the model part
   * @param x the x rotation in degrees
   * @param y the y rotation in degrees
   * @param z the z rotation in degrees
   * @return the AltarPose for chaining instances
   **/
  public AltarPose set(final ModelPart p, final float x, final float y, final float z) {
    return setRadians(p, (float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
  }
  
  /**
   * Adds a model rotation to the AltarPose using radians
   * @param p the model part
   * @param x the x rotation in radians
   * @param y the y rotation in radians
   * @param z the z rotation in radians
   * @return the AltarPose for chaining instances
   **/
  public AltarPose setRadians(final ModelPart p, final float x, final float y, final float z) {
    angles.put(p, new Vector3f(x, y, z));
    return this;
  }
  
  /**
   * @param p the model part
   * @return a vector of 3 floats representing x, y, and z angles in radians
   **/
  public Vector3f get(final ModelPart p) {
    return angles.get(p);
  }
  
  @Override
  public CompoundTag serializeNBT() {
    CompoundTag tag = new CompoundTag();
    for(final Entry<ModelPart, Vector3f> e : angles.entrySet()) {
      final CompoundTag eTag = new CompoundTag();
      eTag.put("x", FloatTag.valueOf(e.getValue().x()));
      eTag.put("y", FloatTag.valueOf(e.getValue().y()));
      eTag.put("z", FloatTag.valueOf(e.getValue().z()));
      tag.put(e.getKey().getSerializedName(), eTag);
    }
    return tag;
  }

  @Override
  public void deserializeNBT(CompoundTag nbt) {
    String key;
    for (final ModelPart m : ModelPart.values()) {
      final CompoundTag eTag = nbt.getCompound(m.getSerializedName());
      float x = 0.0F;
      float y = 0.0F;
      float z = 0.0F;
      if (eTag != null && !eTag.isEmpty()) {
        x = eTag.getFloat("x");
        y = eTag.getFloat("y");
        z = eTag.getFloat("z");
      }
      this.angles.put(m, new Vector3f(x, y, z));
    }
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("AltarPose{\n");
    for(final Entry<ModelPart, Vector3f> entry : angles.entrySet()) {
      builder.append("  ");
      builder.append(entry.getKey().getSerializedName());
      builder.append(" : ");
      builder.append(entry.getValue().toString());
      builder.append("\n");
    }
    return builder.append("}").toString();
  }
}
