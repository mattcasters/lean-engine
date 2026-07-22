package org.lean.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeanGeometryTest {

  @Test
  void containsPointInsideAndOutside() {
    LeanGeometry g = new LeanGeometry(10, 20, 100, 50);
    assertTrue(g.contains(10, 20));
    assertTrue(g.contains(50, 40));
    assertTrue(g.contains(109, 69));
    assertFalse(g.contains(9, 20));
    assertFalse(g.contains(10, 70));
  }

  @Test
  void maxSurfaceExpandsBoundingBox() {
    LeanGeometry a = new LeanGeometry(10, 10, 20, 20);
    LeanGeometry b = new LeanGeometry(5, 15, 40, 30);
    a.maxSurface(b);
    assertEquals(5, a.getX());
    assertEquals(10, a.getY());
    // width/height stored as max of right/bottom edges (existing behavior)
    assertEquals(45, a.getWidth());
    assertEquals(45, a.getHeight());
  }

  @Test
  void equalsAndCopy() {
    LeanGeometry a = new LeanGeometry(1, 2, 3, 4);
    LeanGeometry b = new LeanGeometry(a);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
