package org.lean.core;

import java.awt.Rectangle;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LeanGeometry implements Cloneable {

  @HopMetadataProperty private int x;
  @HopMetadataProperty private int y;
  @HopMetadataProperty private int width;
  @HopMetadataProperty private int height;

  public LeanGeometry(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public LeanGeometry(LeanGeometry componentGeometry) {
    this(
        componentGeometry.x,
        componentGeometry.y,
        componentGeometry.width,
        componentGeometry.height);
  }

  @Override
  public String toString() {
    return "LeanGeometry(" + x + "," + y + ":" + width + "x" + height + ")";
  }

  public boolean contains(int px, int py) {
    return new Rectangle(x, y, width, height).contains(px, py);
  }

  /**
   * Expand this geometry to cover the surface of {@code g} as well (bounding box style).
   */
  public void maxSurface(LeanGeometry g) {
    x = Math.min(x, g.x);
    y = Math.min(y, g.y);
    width = Math.max(width, g.x + g.width);
    height = Math.max(height, g.y + g.height);
  }

  /** Of this geometry or g, keep the lowest. */
  public void lowest(LeanGeometry g) {
    if (y + height < g.y + g.height) {
      x = g.x;
      y = g.y;
      width = g.width;
      height = g.height;
    }
  }

  public void translate(int translateX, int translateY) {
    x += translateX;
    y += translateY;
  }

  public void translate(LeanPosition offSet) {
    translate(offSet.getX(), offSet.getY());
  }

  @Override
  public LeanGeometry clone() {
    return new LeanGeometry(x, y, width, height);
  }

  public void incHeight(int inc) {
    height += inc;
  }

  public void incWidth(int inc) {
    width += inc;
  }

  public void incX(int inc) {
    x += inc;
  }

  public void incY(int inc) {
    y += inc;
  }
}
