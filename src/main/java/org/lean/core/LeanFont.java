package org.lean.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonDeserialize(as = LeanFont.class)
public class LeanFont {

  @HopMetadataProperty @JsonProperty private String fontName;

  @HopMetadataProperty @JsonProperty private String fontSize;

  @HopMetadataProperty @JsonProperty private boolean bold;

  @HopMetadataProperty @JsonProperty private boolean italic;

  public LeanFont(String fontName, String fontSize, boolean bold, boolean italic) {
    this.fontName = fontName;
    this.fontSize = fontSize;
    this.bold = bold;
    this.italic = italic;
  }

  public LeanFont(LeanFont f) {
    this(f.fontName, f.fontSize, f.bold, f.italic);
  }
}
