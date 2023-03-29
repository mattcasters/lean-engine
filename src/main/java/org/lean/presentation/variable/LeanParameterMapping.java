package org.lean.presentation.variable;

import java.util.List;
import org.apache.hop.metadata.api.HopMetadataProperty;

public class LeanParameterMapping {
  @HopMetadataProperty private String connectorName;
  @HopMetadataProperty private List<FieldToParameterMapping> mappings;
  @HopMetadataProperty private String separator;

  public LeanParameterMapping() {}

  public LeanParameterMapping(LeanParameterMapping m) {
    this();
    this.connectorName = m.connectorName;
    m.mappings.forEach(f -> this.mappings.add(new FieldToParameterMapping(f)));
    this.separator = m.separator;
  }

  /**
   * Gets connectorName
   *
   * @return value of connectorName
   */
  public String getConnectorName() {
    return connectorName;
  }

  /**
   * Sets connectorName
   *
   * @param connectorName value of connectorName
   */
  public void setConnectorName(String connectorName) {
    this.connectorName = connectorName;
  }

  /**
   * Gets mappings
   *
   * @return value of mappings
   */
  public List<FieldToParameterMapping> getMappings() {
    return mappings;
  }

  /**
   * Sets mappings
   *
   * @param mappings value of mappings
   */
  public void setMappings(List<FieldToParameterMapping> mappings) {
    this.mappings = mappings;
  }

  /**
   * Gets separator
   *
   * @return value of separator
   */
  public String getSeparator() {
    return separator;
  }

  /**
   * Sets separator
   *
   * @param separator value of separator
   */
  public void setSeparator(String separator) {
    this.separator = separator;
  }

  public static final class FieldToParameterMapping {
    @HopMetadataProperty private String fieldName;
    @HopMetadataProperty private String parameterName;

    public FieldToParameterMapping() {}

    public FieldToParameterMapping(String fieldName, String parameterName) {
      this.fieldName = fieldName;
      this.parameterName = parameterName;
    }

    public FieldToParameterMapping(FieldToParameterMapping m) {
      this();
      this.fieldName = m.fieldName;
      this.parameterName = m.parameterName;
    }

    /**
     * Gets fieldName
     *
     * @return value of fieldName
     */
    public String getFieldName() {
      return fieldName;
    }

    /**
     * Sets fieldName
     *
     * @param fieldName value of fieldName
     */
    public void setFieldName(String fieldName) {
      this.fieldName = fieldName;
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
     * Sets parameterName
     *
     * @param parameterName value of parameterName
     */
    public void setParameterName(String parameterName) {
      this.parameterName = parameterName;
    }
  }
}
