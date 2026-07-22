package org.lean.core.history;

import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
@EqualsAndHashCode(of = {"objectType", "objectName"})
public class LeanUserHistoryAction {
  @HopMetadataProperty private String objectType;

  @HopMetadataProperty private String objectName;

  @HopMetadataProperty private Date actionDate;

  public LeanUserHistoryAction() {
    actionDate = new Date();
  }

  public LeanUserHistoryAction(String objectType, String objectName) {
    this(objectType, objectName, new Date());
  }

  public LeanUserHistoryAction(String objectType, String objectName, Date actionDate) {
    this.objectType = objectType;
    this.objectName = objectName;
    this.actionDate = actionDate;
  }
}
