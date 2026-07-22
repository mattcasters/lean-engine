package org.lean.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.hop.metadata.api.HopMetadataProperty;

/** To attach the location of one component to another */
@Getter
@Setter
@ToString
public class LeanAttachment {

  @HopMetadataProperty private String componentName;
  @HopMetadataProperty private int percentage;
  @HopMetadataProperty private int offset;
  @HopMetadataProperty private Alignment alignment;

  public LeanAttachment() {
    componentName = null;
    percentage = 0;
    offset = 0;
    alignment = Alignment.DEFAULT;
  }

  public LeanAttachment(int percentage, int offset) {
    this.percentage = percentage;
    this.offset = offset;
    alignment = Alignment.DEFAULT;
  }

  public LeanAttachment(String componentName, int percentage, int offset) {
    this.componentName = componentName;
    this.percentage = percentage;
    this.offset = offset;
    alignment = Alignment.DEFAULT;
  }

  public LeanAttachment(String componentName, int percentage, int offset, Alignment alignment) {
    this.componentName = componentName;
    this.percentage = percentage;
    this.offset = offset;
    this.alignment = alignment;
  }

  public LeanAttachment(LeanAttachment attachment) {
    this.componentName = attachment.componentName;
    this.percentage = attachment.percentage;
    this.offset = attachment.offset;
    this.alignment = attachment.alignment;
  }

  public enum Alignment {
    DEFAULT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CENTER,
  }
}
