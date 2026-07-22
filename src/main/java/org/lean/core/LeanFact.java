package org.lean.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
public class LeanFact extends LeanColumn {

  @HopMetadataProperty private AggregationMethod aggregationMethod;

  @HopMetadataProperty private boolean horizontalAggregation;

  @HopMetadataProperty private String horizontalAggregationHeader;

  @HopMetadataProperty private boolean verticalAggregation;

  @HopMetadataProperty private String verticalAggregationHeader;

  @Getter(AccessLevel.NONE)
  @HopMetadataProperty
  private LeanHorizontalAlignment headerHorizontalAlignment;

  @Getter(AccessLevel.NONE)
  @HopMetadataProperty
  private LeanVerticalAlignment headerVerticalAlignment;

  public LeanFact() {
    super();
    headerHorizontalAlignment = LeanHorizontalAlignment.LEFT;
    headerVerticalAlignment = LeanVerticalAlignment.TOP;
  }

  public LeanFact(String columnName, AggregationMethod aggregationMethod) {
    super(columnName);
    this.aggregationMethod = aggregationMethod;
  }

  public LeanFact(
      String columnName,
      String headerValue,
      LeanHorizontalAlignment horizontalAlignment,
      LeanVerticalAlignment verticalAlignment,
      AggregationMethod aggregationMethod,
      String formatMask) {
    super(columnName, headerValue, horizontalAlignment, verticalAlignment);
    this.aggregationMethod = aggregationMethod;
    setFormatMask(formatMask);
  }

  public LeanFact(LeanFact f) {
    super(f);
    this.aggregationMethod = f.aggregationMethod;
    this.horizontalAggregation = f.horizontalAggregation;
    this.horizontalAggregationHeader = f.horizontalAggregationHeader;
    this.verticalAggregation = f.verticalAggregation;
    this.verticalAggregationHeader = f.verticalAggregationHeader;
    this.headerHorizontalAlignment = f.headerHorizontalAlignment;
    this.headerVerticalAlignment = f.headerVerticalAlignment;
  }

  public LeanHorizontalAlignment getHeaderHorizontalAlignment() {
    return headerHorizontalAlignment != null
        ? headerHorizontalAlignment
        : LeanHorizontalAlignment.LEFT;
  }

  public LeanVerticalAlignment getHeaderVerticalAlignment() {
    return headerVerticalAlignment != null ? headerVerticalAlignment : LeanVerticalAlignment.TOP;
  }
}
