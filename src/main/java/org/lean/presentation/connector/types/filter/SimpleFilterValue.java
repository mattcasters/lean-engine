package org.lean.presentation.connector.types.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@JsonDeserialize(as = LeanSimpleFilterConnector.class)
@Getter
@Setter
public class SimpleFilterValue {

  private String fieldName;

  private String filterValue;

  public SimpleFilterValue() {}

  public SimpleFilterValue(String fieldName, String filterValue) {
    this.fieldName = fieldName;
    this.filterValue = filterValue;
  }
}
