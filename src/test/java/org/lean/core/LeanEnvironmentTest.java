package org.lean.core;

import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.IPluginType;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.LeanConnectorPluginType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LeanEnvironmentTest {

  @BeforeEach
  public void before() throws Exception {

    MemoryMetadataProvider metadataProvider = new MemoryMetadataProvider();
    IVariables variables = Variables.getADefaultVariableSpace();

    // Load all plugins, initialize environment
    //
    LeanEnvironment.init();
  }

  @Test
  public void testInit() throws Exception {
    PluginRegistry registry = PluginRegistry.getInstance();

    // Check Component plugin type...
    //
    IPluginType leanComponentPluginType = registry.getPluginType(LeanComponentPluginType.class);
    assertNotNull(leanComponentPluginType, "Component plugin type not found");
    IPlugin leanLabelComponent =
        registry.findPluginWithId(LeanComponentPluginType.class, "LeanLabelComponent");
    assertNotNull(leanLabelComponent, "Label component not found");

    List<IPlugin> componentPlugins = registry.getPlugins(LeanComponentPluginType.class);
    assertTrue(!componentPlugins.isEmpty(), "Plugins list empty");

    // Check connector plugin type...
    //
    IPluginType leanConnectorPluginType = registry.getPluginType(LeanConnectorPluginType.class);
    assertNotNull(leanConnectorPluginType, "Data connector plugin type not found");
    IPlugin sampleDataConnector =
        registry.findPluginWithId(LeanConnectorPluginType.class, "SampleDataConnector");
    assertNotNull(sampleDataConnector, "Sample data connector plugin type not found");

    List<IPlugin> connectorPlugins = registry.getPlugins(LeanConnectorPluginType.class);
    assertTrue(!connectorPlugins.isEmpty(), "Plugins list empty");
  }
}
