package rpggods.altar;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.Map.Entry;

public class AltarPose implements INBTSerializable<CompoundNBT> {

  public static final AltarPose EMPTY = new AltarPose();

  public static final Codec<AltarPose> CODEC = CompoundNBT.CODEC.xmap(AltarPose::new, AltarPose::serializeNBT);

  private static final String KEY_ANGLES = "angles";
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
  
  public AltarPose(final CompoundNBT tag) {
    deserializeNBT(tag);
  }
  
  /**
   * Adds a model rotation to the statuePose using degrees
   * @param p the model part
   * @param x the x rotation in degrees
   * @param y the y rotation in degrees
   * @param z the z rotation in degrees
   * @return the StatuePose for chaining instances
   **/
  public AltarPose set(final ModelPart p, final float x, final float y, final float z) {
    return setRadians(p, (float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
  }
  
  /**
   * Adds a model rotation to the statuePose using radians
   * @param p the model part
   * @param x the x rotation in radians
   * @param y the y rotation in radians
   * @param z the z rotation in radians
   * @return the StatuePose for chaining instances
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
  public CompoundNBT serializeNBT() {
    CompoundNBT tag = new CompoundNBT();
    for(final Entry<ModelPart, Vector3f> e : angles.entrySet()) {
      final CompoundNBT eTag = new CompoundNBT();
      eTag.put("x", FloatNBT.valueOf(e.getValue().getX()));
      eTag.put("y", FloatNBT.valueOf(e.getValue().getY()));
      eTag.put("z", FloatNBT.valueOf(e.getValue().getZ()));
      tag.put(e.getKey().getString(), eTag);
    }
    return tag;
  }

  @Override
  public void deserializeNBT(CompoundNBT nbt) {
    String key;
    for (final ModelPart m : ModelPart.values()) {
      final CompoundNBT eTag = nbt.getCompound(m.getString());
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
      builder.append(entry.getKey().getString());
      builder.append(" : ");
      builder.append(entry.getValue().toString());
      builder.append("\n");
    }
    return builder.append("}").toString();
  }
}
