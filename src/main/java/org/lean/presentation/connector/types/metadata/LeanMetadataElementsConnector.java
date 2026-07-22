package org.lean.presentation.connector.types.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMetaBuilder;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadata;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.lean.core.ILeanRowListener;
import org.lean.core.exception.LeanException;
import org.lean.core.gui.form.LeanGuiFormConstants;
import org.lean.core.gui.plugin.LeanWidgetElement;
import org.lean.core.gui.plugin.LeanWidgetType;
import org.lean.presentation.connector.type.ILeanConnector;
import org.lean.presentation.connector.type.LeanBaseConnector;
import org.lean.presentation.connector.type.LeanConnectorPlugin;
import org.lean.presentation.datacontext.IDataContext;
import lombok.Getter;
import lombok.Setter;

@JsonDeserialize(as = LeanMetadataElementsConnector.class)
@LeanConnectorPlugin(
    id = "MetadataElementsConnector",
    name = "Metadata elements",
    description = "Lists the available metadata elements")
@Getter
@Setter
public class LeanMetadataElementsConnector extends LeanBaseConnector implements ILeanConnector {

  @LeanWidgetElement(
      order = "10000-elementKey",
      parentId = LeanGuiFormConstants.PARENT_PLUGIN,
      type = LeanWidgetType.TEXT,
      label = "Metadata type key",
      toolTip = "Hop metadata key (e.g. presentation, theme, lean-database-connection)")
  @HopMetadataProperty
  private String elementKey;

  public LeanMetadataElementsConnector() {
    super("MetadataElementsConnector");
  }

  public LeanMetadataElementsConnector(String elementKey) {
    this();
    this.elementKey = elementKey;
  }

  public LeanMetadataElementsConnector(LeanMetadataElementsConnector c) {
    super(c);
    this.elementKey = c.elementKey;
  }

  public LeanMetadataElementsConnector clone() {
    return new LeanMetadataElementsConnector(this);
  }

  @Override
  public IRowMeta describeOutput(IDataContext dataContext) throws LeanException {
    return new RowMetaBuilder().addString("name").build();
  }

  /**
   * Output the names of the elements for the given key
   *
   * @param dataContext the data context to reference
   * @throws LeanException
   */
  @Override
  public void startStreaming(IDataContext dataContext) throws LeanException {
    IRowMeta rowMeta = describeOutput(dataContext);

    if (StringUtils.isEmpty(elementKey)) {
      throw new LeanException("Please specify the key of the metadata element type to list");
    }

    try {
      IHopMetadataProvider provider = dataContext.getMetadataProvider();
      Class<IHopMetadata> hopMetadataClass = provider.getMetadataClassForKey(elementKey);
      IHopMetadataSerializer<IHopMetadata> serializer = provider.getSerializer(hopMetadataClass);

      List<String> names = serializer.listObjectNames();

      for (String name : names) {

        Object[] rowData = RowDataUtil.allocateRowData(rowMeta.size());
        rowData[0] = name;

        for (ILeanRowListener rowListener : rowListeners) {
          rowListener.rowReceived(rowMeta, rowData);
        }
      }
    } catch (Exception e) {
      throw new LeanException("Error writing metadata elements output", e);
    }

    // Signal to all row listeners that no more rows are forthcoming.
    //
    outputDone();
  }

  @Override
  public void waitUntilFinished() throws LeanException {
    // StartStreaming works synchronized, no need to get complicated about it
  }
}
