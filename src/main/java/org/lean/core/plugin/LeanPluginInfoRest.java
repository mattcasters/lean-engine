package org.lean.core.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.IPluginType;
import org.apache.hop.core.plugins.PluginRegistry;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanConnectorPluginType;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("plugins/")
public class LeanPluginInfoRest {

  private static List<LeanPluginDescription> componentPlugins;
  private static List<LeanPluginDescription> connectorPlugins;

  /**
   * List all component types
   *
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  @JsonIgnore
  @Path("components")
  public List<LeanPluginDescription> listComponents() {
    try {
      if (componentPlugins == null) {
        componentPlugins =
            getPluginDescriptions(LeanComponentPluginType.class, ILeanComponent.class);
      }
      return componentPlugins;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500); // General error
    }
  }

  /**
   * List all connector types
   *
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  @JsonIgnore
  @Path("connectors")
  public List<LeanPluginDescription> listConnectors() {
    try {
      if (connectorPlugins == null) {
        connectorPlugins =
            getPluginDescriptions(LeanConnectorPluginType.class, ILeanConnector.class);
      }
      return connectorPlugins;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500); // General error
    }
  }

  private List<LeanPluginDescription> getPluginDescriptions(
      Class<? extends IPluginType> pluginTypeClass, Class<?> interfaceClass) {
    PluginRegistry registry = PluginRegistry.getInstance();

    List<IPlugin> plugins = registry.getPlugins(pluginTypeClass);
    List<LeanPluginDescription> list = new ArrayList<LeanPluginDescription>();
    for (IPlugin plugin : plugins) {
      list.add(
          new LeanPluginDescription(
              plugin.getIds()[0],
              plugin.getName(),
              plugin.getDescription(),
              plugin.getClassMap().get(interfaceClass),
              plugin.getLibraries()));
    }
    Collections.sort(list);
    return list;
  }
}
