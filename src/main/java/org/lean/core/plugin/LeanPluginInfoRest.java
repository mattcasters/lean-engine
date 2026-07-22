package org.lean.core.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.IPluginType;
import org.apache.hop.core.plugins.PluginRegistry;
import org.lean.presentation.component.type.ILeanComponent;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanConnectorPluginType;

/**
 * Lists registered Lean component and connector plugins for REST clients.
 *
 * <p>Returns pre-serialized JSON strings so Jersey does not need entity-filtering Jackson
 * providers (avoids NPEs with jersey-media-json-jackson filtering).
 */
@Path("plugins/")
public class LeanPluginInfoRest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static List<LeanPluginDescription> componentPlugins;
  private static List<LeanPluginDescription> connectorPlugins;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("components")
  public Response listComponents() {
    try {
      if (componentPlugins == null) {
        componentPlugins =
            getPluginDescriptions(LeanComponentPluginType.class, ILeanComponent.class);
      }
      return json(componentPlugins);
    } catch (Exception e) {
      return Response.serverError()
          .entity("Error listing component plugins: " + e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("connectors")
  public Response listConnectors() {
    try {
      if (connectorPlugins == null) {
        connectorPlugins =
            getPluginDescriptions(LeanConnectorPluginType.class, ILeanConnector.class);
      }
      return json(connectorPlugins);
    } catch (Exception e) {
      return Response.serverError()
          .entity("Error listing connector plugins: " + e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
  }

  private static Response json(Object value) throws Exception {
    return Response.ok(MAPPER.writeValueAsString(value))
        .type(MediaType.APPLICATION_JSON_TYPE)
        .encoding("UTF-8")
        .build();
  }

  private List<LeanPluginDescription> getPluginDescriptions(
      Class<? extends IPluginType> pluginTypeClass, Class<?> interfaceClass) {
    PluginRegistry registry = PluginRegistry.getInstance();

    List<IPlugin> plugins = registry.getPlugins(pluginTypeClass);
    List<LeanPluginDescription> list = new ArrayList<>();
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
