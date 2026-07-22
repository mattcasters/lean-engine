package org.lean.core;

import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
public class LeanColumn {

  @HopMetadataProperty private String columnName;

  @HopMetadataProperty private String headerValue;

  @HopMetadataProperty private LeanHorizontalAlignment horizontalAlignment;

  @HopMetadataProperty private LeanVerticalAlignment verticalAlignment;

  @HopMetadataProperty private int width;

  @HopMetadataProperty private String formatMask;

  @HopMetadataProperty @Deprecated private LeanFont font;

  public LeanColumn() {
    horizontalAlignment = LeanHorizontalAlignment.LEFT;
    verticalAlignment = LeanVerticalAlignment.TOP;
  }

  public LeanColumn(LeanColumn c) {
    this.columnName = c.columnName;
    this.headerValue = c.headerValue;
    this.horizontalAlignment = c.horizontalAlignment;
    this.verticalAlignment = c.verticalAlignment;
    this.width = c.width;
    this.formatMask = c.formatMask;
  }

  public LeanColumn(String columnName) {
    this();
    this.columnName = columnName;
  }

  public LeanColumn(
      String columnName,
      String headerValue,
      LeanHorizontalAlignment horizontalAlignment,
      LeanVerticalAlignment verticalAlignment) {
    this.columnName = columnName;
    this.headerValue = headerValue;
    this.horizontalAlignment = horizontalAlignment;
    this.verticalAlignment = verticalAlignment;
  }
}
