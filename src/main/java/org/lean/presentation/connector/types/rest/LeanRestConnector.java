package org.lean.presentation.connector.types.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.encryption.Encr;
import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.metadata.serializer.json.JsonMetadataProvider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.lean.core.ILeanRowListener;
import org.lean.core.LeanEnvironment;
import org.lean.core.exception.LeanException;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.datacontext.IDataContext;

@JsonDeserialize(as = LeanRestConnector.class)
@LeanConnectorPlugin(
    id = "LeanRestConnector",
    name = "REST",
    description = "This connector retrieves and parses JSON data from a REST service")
public class LeanRestConnector extends LeanBaseConnector implements ILeanConnector {
  @HopMetadataProperty private String url;

  @HopMetadataProperty private String path;

  @HopMetadataProperty private String body;

  @HopMetadataProperty private String rowsElement;

  @HopMetadataProperty(key = "fields")
  private List<JsonField> fields;

  public LeanRestConnector() {
    super("LeanRestConnector");
    this.fields = new ArrayList<>();
  }

  public LeanRestConnector(LeanRestConnector c) {
    this();
    this.url = c.url;
    this.path = c.path;
    this.body = c.body;
    this.rowsElement = c.rowsElement;
    c.fields.forEach(f -> this.fields.add(new JsonField(f)));
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    try {
      IRowMeta rowMeta = new RowMeta();
      for (JsonField field : fields) {
        rowMeta.addValueMeta(field.createValueMeta());
      }
      return rowMeta;
    } catch (Exception e) {
      throw new LeanException("Error describing output of the REST connector", e);
    }
  }

  @Override
  public LeanRestConnector clone() {
    return new LeanRestConnector(this);
  }

  @Override
  public void startStreaming(IDataContext dataContext) throws LeanException {
    IVariables variables = dataContext.getVariables();
    IRowMeta rowMeta = describeOutput(dataContext);
    HttpClientBuilder builder = HttpClientBuilder.create();
    builder.setConnectionTimeToLive(5, TimeUnit.MINUTES);
    HttpClient httpClient = builder.build();

    String fullUrl = variables.resolve(url + path);

    try {
      HttpPost request = new HttpPost(fullUrl);

      // Accepts and produces JSON
      //
      request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
      request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

      // Send the body (if any).
      //
      if (StringUtils.isNotEmpty(body)) {
        request.setEntity(new StringEntity(variables.resolve(body)));
      }

      HttpResponse response = httpClient.execute(request);

      // verify the valid error code first
      //
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new LeanException(
            "REST call failed with HTTP error code : "
                + statusCode
                + " : "
                + response.getStatusLine().getReasonPhrase());
      }
      HttpEntity httpEntity = response.getEntity();
      String json = EntityUtils.toString(httpEntity);

      // Parse the JSON response
      //
      JSONParser parser = new JSONParser();
      JSONObject jsonObject;
      try {
        jsonObject = (JSONObject) parser.parse(json);
      } catch (Exception e) {
        throw new LeanException("Error parsing JSON body: " + json, e);
      }

      // Get the rows object...
      //
      String realRowsElement = variables.resolve(rowsElement);
      JSONArray rowElements;
      Object elements = jsonObject.get(realRowsElement);
      if (elements == null) {
        throw new LeanException(
            "Unable to find rows element '" + realRowsElement + "' in JSON: " + json);
      }
      try {
        if (elements instanceof JSONObject) {
          rowElements = new JSONArray();
          rowElements.add(elements);
        } else if (elements instanceof JSONArray) {
          rowElements = (JSONArray) jsonObject.get(realRowsElement);
        } else {
          throw new LeanException("Expected an array of rows in JSON: " + json);
        }
      } catch (Exception e) {
        throw new LeanException(
            "Error getting array of values for rows element "
                + rowsElement
                + " in request JSON: "
                + json,
            e);
      }

      Iterator<JSONObject> iterator = rowElements.iterator();
      while (iterator.hasNext()) {
        JSONObject rowObject = iterator.next();

        Object[] rowData = RowDataUtil.allocateRowData(rowMeta.size());
        for (int i = 0; i < rowMeta.size(); i++) {
          IValueMeta valueMeta = rowMeta.getValueMeta(i);
          JsonField field = fields.get(i);
          Object value = rowObject.get(field.getTag());

          if (value != null) {
            switch (valueMeta.getType()) {
              case IValueMeta.TYPE_STRING:
                rowData[i] = value.toString();
                break;
              case IValueMeta.TYPE_INTEGER:
                rowData[i] = Long.parseLong(value.toString());
                break;
              default:
                throw new LeanException(
                    "Data type "
                        + valueMeta.getTypeDesc()
                        + " isn't supported yet for tag: '"
                        + field.getTag()
                        + "', value class: "
                        + value.getClass()
                        + ", value itself: "
                        + value);
            }
          }
        }

        // Pass the row to the listeners
        //
        for (ILeanRowListener rowListener : rowListeners) {
          rowListener.rowReceived(rowMeta, rowData);
        }
      }

      // Signal to all row listeners that no more rows are forthcoming.
      //
      outputDone();
    } catch (Exception e) {
      throw new LeanException("Error getting data from REST service URL " + fullUrl, e);
    }
  }

  @Override
  public void waitUntilFinished() throws LeanException {
    // This is not an asynchronous connector. There's no need to wait for anything.
  }

  /**
   * Gets url
   *
   * @return value of url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url
   *
   * @param url value of url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets path
   *
   * @return value of path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets path
   *
   * @param path value of path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Gets rowsElement
   *
   * @return value of rowsElement
   */
  public String getRowsElement() {
    return rowsElement;
  }

  /**
   * Sets rowsElement
   *
   * @param rowsElement value of rowsElement
   */
  public void setRowsElement(String rowsElement) {
    this.rowsElement = rowsElement;
  }

  /**
   * Gets fields
   *
   * @return value of fields
   */
  public List<JsonField> getFields() {
    return fields;
  }

  /**
   * Sets fields
   *
   * @param fields value of fields
   */
  public void setFields(List<JsonField> fields) {
    this.fields = fields;
  }

  /**
   * Gets body
   *
   * @return value of body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets body
   *
   * @param body value of body
   */
  public void setBody(String body) {
    this.body = body;
  }

  public static final class JsonField {
    @HopMetadataProperty private String tag;
    @HopMetadataProperty private String name;
    @HopMetadataProperty private String type;
    @HopMetadataProperty private String formatMask;
    @HopMetadataProperty private String length;
    @HopMetadataProperty private String precision;
    @HopMetadataProperty private String decimal;
    @HopMetadataProperty private String grouping;

    public JsonField() {}

    public JsonField(JsonField f) {
      this.tag = f.tag;
      this.name = f.name;
      this.type = f.type;
      this.formatMask = f.formatMask;
      this.length = f.length;
      this.precision = f.precision;
      this.decimal = f.decimal;
      this.grouping = f.grouping;
    }

    public JsonField(String name, String type) {
      this();
      this.name = name;
      this.type = type;
    }

    public IValueMeta createValueMeta() throws HopPluginException {
      int hopType = ValueMetaFactory.getIdForValueMeta(type);
      IValueMeta valueMeta = ValueMetaFactory.createValueMeta(Const.NVL(name, tag), hopType);
      valueMeta.setLength(Const.toInt(length, -1));
      valueMeta.setPrecision(Const.toInt(precision, -1));
      valueMeta.setConversionMask(formatMask);
      valueMeta.setDecimalSymbol(decimal);
      valueMeta.setGroupingSymbol(grouping);
      return valueMeta;
    }

    /**
     * Gets tag
     *
     * @return value of tag
     */
    public String getTag() {
      return tag;
    }

    /**
     * Sets tag
     *
     * @param tag value of tag
     */
    public void setTag(String tag) {
      this.tag = tag;
    }

    /**
     * Gets name
     *
     * @return value of name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name
     *
     * @param name value of name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets type
     *
     * @return value of type
     */
    public String getType() {
      return type;
    }

    /**
     * Sets type
     *
     * @param type value of type
     */
    public void setType(String type) {
      this.type = type;
    }

    /**
     * Gets formatMask
     *
     * @return value of formatMask
     */
    public String getFormatMask() {
      return formatMask;
    }

    /**
     * Sets formatMask
     *
     * @param formatMask value of formatMask
     */
    public void setFormatMask(String formatMask) {
      this.formatMask = formatMask;
    }

    /**
     * Gets length
     *
     * @return value of length
     */
    public String getLength() {
      return length;
    }

    /**
     * Sets length
     *
     * @param length value of length
     */
    public void setLength(String length) {
      this.length = length;
    }

    /**
     * Gets precision
     *
     * @return value of precision
     */
    public String getPrecision() {
      return precision;
    }

    /**
     * Sets precision
     *
     * @param precision value of precision
     */
    public void setPrecision(String precision) {
      this.precision = precision;
    }

    /**
     * Gets decimal
     *
     * @return value of decimal
     */
    public String getDecimal() {
      return decimal;
    }

    /**
     * Sets decimal
     *
     * @param decimal value of decimal
     */
    public void setDecimal(String decimal) {
      this.decimal = decimal;
    }

    /**
     * Gets grouping
     *
     * @return value of grouping
     */
    public String getGrouping() {
      return grouping;
    }

    /**
     * Sets grouping
     *
     * @param grouping value of grouping
     */
    public void setGrouping(String grouping) {
      this.grouping = grouping;
    }
  }

  public static void main(String[] args) throws Exception {
    LeanEnvironment.init();
    LeanRestConnector c = new LeanRestConnector();
    c.setUrl("http://localhost:8081");
    c.setPath("/hop/api/v1/execute/sync/");
    c.setBody("{ \"service\" : \"list-executions\", \"runConfig\" : \"local\" }");
    c.setRowsElement("rows");
    c.getFields()
        .addAll(
            Arrays.asList(
                new JsonField("executionId", "String"),
                new JsonField("name", "String"),
                new JsonField("executionType", "String"),
                new JsonField("filename", "String")));
    LeanConnector connector = new LeanConnector("rest", c);

    JsonMetadataProvider provider =
        new JsonMetadataProvider(
            Encr.getEncoder(), "/tmp/metadata/", Variables.getADefaultVariableSpace());
    IHopMetadataSerializer<LeanConnector> serializer = provider.getSerializer(LeanConnector.class);
    serializer.save(connector);
  }
}
