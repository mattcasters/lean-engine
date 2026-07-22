package org.lean.core.history;

import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadata;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@HopMetadata(
    key = "user-history",
    name = "Lean User History",
    description = "Describes user action history")
@Getter
@Setter
public class LeanUserHistory extends HopMetadataBase implements IHopMetadata {

  @HopMetadataProperty private List<LeanUserHistoryAction> actions;

  public LeanUserHistory() {
    actions = new ArrayList<>();
  }

  public LeanUserHistory(String name, List<LeanUserHistoryAction> actions) {
    this.name = name;
    this.actions = actions;
  }
}
