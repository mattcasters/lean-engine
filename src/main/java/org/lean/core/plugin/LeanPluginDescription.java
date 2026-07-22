package org.lean.core.plugin;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeanPluginDescription implements Comparable<LeanPluginDescription> {
  private String id;
  private String name;
  private String description;
  private String className;
  private List<String> libraries;

  public LeanPluginDescription() {
    libraries = new ArrayList<>();
  }

  public LeanPluginDescription(
      String id, String name, String description, String className, List<String> libraries) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.className = className;
    this.libraries = libraries;
  }

  @Override
  public int compareTo(LeanPluginDescription leanPluginDescription) {
    return id.compareTo(leanPluginDescription.id);
  }
}
