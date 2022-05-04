package rpggods.altar;

import net.minecraft.util.StringRepresentable;

public enum ModelPart implements StringRepresentable {
  HEAD("head"), 
  BODY("body"), 
  LEFT_ARM("left_arm"), 
  RIGHT_ARM("right_arm"), 
  LEFT_LEG("left_leg"), 
  RIGHT_LEG("right_leg"),
  OFFSET("offset");
  
  private String name;
  private ModelPart(final String n) {
    this.name = n;
  }
  
  @Override
  public String getSerializedName() {
    return name;
  }
}
