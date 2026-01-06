package org.leralix.tan.dataclass;
public record Range(int minVal, int maxVal) {
  public boolean isValueIn(int value) {
    return minVal <= value && value <= maxVal;
  }
  public int getMinVal() {
    return minVal;
  }
  public int getMaxVal() {
    return maxVal;
  }
}