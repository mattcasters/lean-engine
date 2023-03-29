package org.lean.presentation.variable;

import java.util.Objects;
import org.apache.hop.metadata.api.HopMetadataProperty;

/**
 * Defines a parameter for a lean presentation.
 *
 * <p>TODO: Add different types of parameters: Static value (current), from connector field, from
 * prompt, ...
 *
 * <p>TODO: add prompts as a source type TODO: move the source type to a separate plugin type
 */
public class LeanParameter {

  @HopMetadataProperty private String parameterName;
  @HopMetadataProperty private String parameterValue;

  public LeanParameter() {}

  /**
   * Create a new parameter of type static value
   *
   * @param parameterName
   * @param parameterValue
   */
  public LeanParameter(String parameterName, String parameterValue) {
    this.parameterName = parameterName;
    this.parameterValue = parameterValue;
  }

  public LeanParameter(LeanParameter v) {
    this.parameterName = v.parameterName;
    this.parameterValue = v.parameterValue;
  }

  @Override
  public String toString() {
    return "LeanParameter{"
        + "parameterName='"
        + parameterName
        + '\''
        + ", parameterValue='"
        + parameterValue
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LeanParameter parameter = (LeanParameter) o;
    return Objects.equals(parameterName, parameter.parameterName)
        && Objects.equals(parameterValue, parameter.parameterValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameterName, parameterValue);
  }

  /**
   * Gets parameterName
   *
   * @return value of parameterName
   */
  public String getParameterName() {
    return parameterName;
  }

  /**
   * @param parameterName The parameterName to set
   */
  public void setParameterName(String parameterName) {
    this.parameterName = parameterName;
  }

  /**
   * Gets parameterValue
   *
   * @return value of parameterValue
   */
  public String getParameterValue() {
    return parameterValue;
  }

  /**
   * @param parameterValue The parameterValue to set
   */
  public void setParameterValue(String parameterValue) {
    this.parameterValue = parameterValue;
  }
}
