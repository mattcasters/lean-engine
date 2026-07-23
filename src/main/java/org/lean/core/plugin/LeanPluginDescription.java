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

  /**
   * Classpath path to the plugin icon resource (from {@code @LeanConnectorPlugin#image()} / Hop
   * {@code IPlugin#getImageFile()}). Empty if none. Clients should load bytes via {@code GET
   * plugins/connectors/{id}/image}.
   */
  private String image;

  public LeanPluginDescription() {
    libraries = new ArrayList<>();
  }

  public LeanPluginDescription(
      String id, String name, String description, String className, List<String> libraries) {
    this(id, name, description, className, libraries, null);
  }

  public LeanPluginDescription(
      String id,
      String name,
      String description,
      String className,
      List<String> libraries,
      String image) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.className = className;
    this.libraries = libraries != null ? libraries : new ArrayList<>();
    this.image = image;
  }

  @Override
  public int compareTo(LeanPluginDescription leanPluginDescription) {
    return id.compareTo(leanPluginDescription.id);
  }
}
