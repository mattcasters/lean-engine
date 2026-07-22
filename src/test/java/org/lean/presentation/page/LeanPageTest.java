package org.lean.presentation.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.lean.core.exception.LeanException;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.layout.LeanLayoutBuilder;

class LeanPageTest {

  @Test
  void testGetSortedComponents() throws LeanException {
    LeanPage page = new LeanPage();

    {
      LeanComponent component = new LeanComponent("D", null);
      component.setLayout(new LeanLayoutBuilder().below("C", 0).build());
      page.getComponents().add(component);
    }

    {
      LeanComponent component = new LeanComponent("C", null);
      component.setLayout(new LeanLayoutBuilder().below("B", 0).build());
      page.getComponents().add(component);
    }

    {
      LeanComponent component = new LeanComponent("B", null);
      component.setLayout(new LeanLayoutBuilder().below("A", 0).build());
      page.getComponents().add(component);
    }

    {
      LeanComponent component = new LeanComponent("A", null);
      component.setLayout(new LeanLayoutBuilder().top().left().build());
      page.getComponents().add(component);
    }

    {
      LeanComponent component = new LeanComponent("C2", null);
      component.setLayout(new LeanLayoutBuilder().beside("C1", 5).build());
      page.getComponents().add(component);
    }

    {
      LeanComponent component = new LeanComponent("C1", null);
      component.setLayout(new LeanLayoutBuilder().beside("C", 5).build());
      page.getComponents().add(component);
    }

    {
      LeanComponent component = new LeanComponent("E", null);
      component.setLayout(new LeanLayoutBuilder().top().right().build());
      page.getComponents().add(component);
    }

    List<LeanComponent> sortedComponents = page.getSortedComponents();
    verifySortedList(sortedComponents);

    final Random random = new Random(42);
    for (int i = 0; i < 1000; i++) {
      Collections.shuffle(page.getComponents(), random);
      verifySortedList(page.getSortedComponents());
    }
  }

  private void verifySortedList(List<LeanComponent> sortedComponents) {
    StringBuilder order = new StringBuilder();
    for (LeanComponent component : sortedComponents) {
      order.append(component.getName());
    }
    String orderString = order.toString();
    Set<String> possibilities =
        new HashSet<>(Arrays.asList("AEBCC1C2D", "AEBCDC1C2", "AEBCC1DC2"));
    assertTrue(possibilities.contains(orderString), orderString + " not valid");
  }
}
