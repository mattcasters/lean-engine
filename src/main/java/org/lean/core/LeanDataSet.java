package org.lean.core;

import org.apache.hop.core.row.IRowMeta;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeanDataSet {
  private IRowMeta rowMeta;

  private List<Object[]> rows;

  public LeanDataSet() {
    rows = new ArrayList<>();
  }

  public LeanDataSet(IRowMeta rowMeta, List<Object[]> rows) {
    this.rowMeta = rowMeta;
    this.rows = rows;
  }
}
