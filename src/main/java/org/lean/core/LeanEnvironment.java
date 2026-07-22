package org.lean.core;

import org.apache.hop.core.HopClientEnvironment;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.metadata.plugin.MetadataPluginType;
import org.lean.core.exception.LeanException;
import org.lean.presentation.component.type.LeanComponentPluginType;
import org.lean.presentation.connector.type.LeanConnectorPluginType;

/**
 * Initializes Hop and registers Lean component/connector (and metadata) plugin types.
 *
 * <p>Safe to call repeatedly and from multiple threads; initialization runs once.
 */
public class LeanEnvironment {

  /** Has the Lean environment been initialized? */
  private static volatile boolean initialized;

  private LeanEnvironment() {
    // utility
  }

  public static synchronized void init() throws LeanException {
    if (initialized) {
      return;
    }

    try {
      if (!HopClientEnvironment.isInitialized()) {
        HopClientEnvironment.init();
      }
    } catch (HopException e) {
      throw new LeanException("Unable to initialize the Hop client API environment", e);
    }

    try {
      // MetadataPluginType is not part of the default HopClientEnvironment plugin set.
      PluginRegistry.addPluginType(MetadataPluginType.getInstance());
      PluginRegistry.addPluginType(LeanComponentPluginType.getInstance());
      PluginRegistry.addPluginType(LeanConnectorPluginType.getInstance());
      // registerType() skips types already loaded; only new Lean/metadata types are scanned.
      PluginRegistry.init();
    } catch (Exception e) {
      throw new LeanException("Unable to register lean plugin types", e);
    }

    initialized = true;
  }

  public static boolean isInitialized() {
    return initialized;
  }
}
