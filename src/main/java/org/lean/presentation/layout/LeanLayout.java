package org.lean.presentation.layout;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.lean.core.LeanAttachment;
import org.lean.core.exception.LeanException;
import org.lean.presentation.component.LeanComponent;

/**
 * In case a position is not relative it means absolute vs the top and left margins of the page. In
 * that situation, you simply set or get the (x,y) position and you're done.
 *
 * <p>In case the position is relative versus another component, you need to provide a bunch of
 * details for the x and y coordinates.
 */
@Getter
@Setter
@NoArgsConstructor
public class LeanLayout {

  @HopMetadataProperty private LeanAttachment left;
  @HopMetadataProperty private LeanAttachment right;
  @HopMetadataProperty private LeanAttachment top;
  @HopMetadataProperty private LeanAttachment bottom;

  public LeanLayout(int x, int y) {
    left = new LeanAttachment(0, x);
    top = new LeanAttachment(0, y);
  }

  public LeanLayout(
      LeanAttachment left, LeanAttachment right, LeanAttachment top, LeanAttachment bottom) {
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
  }

  public LeanLayout(LeanLayout layout) {
    this.left = layout.left == null ? null : new LeanAttachment(layout.left);
    this.right = layout.right == null ? null : new LeanAttachment(layout.right);
    this.top = layout.top == null ? null : new LeanAttachment(layout.top);
    this.bottom = layout.bottom == null ? null : new LeanAttachment(layout.bottom);
  }

  public static LeanLayout topLeftPage() {
    LeanLayout layout = new LeanLayout();
    layout.left = new LeanAttachment(null, 0, 0, LeanAttachment.Alignment.LEFT);
    layout.top = new LeanAttachment(null, 0, 0, LeanAttachment.Alignment.TOP);
    return layout;
  }

  public static LeanLayout under(String otherComponent, boolean spanPageWidth) {
    LeanLayout layout = new LeanLayout();
    layout.left = new LeanAttachment(otherComponent, 0, 0, LeanAttachment.Alignment.LEFT);
    layout.top = new LeanAttachment(otherComponent, 0, 0, LeanAttachment.Alignment.BOTTOM);
    if (spanPageWidth) {
      layout.right = new LeanAttachment(null, 0, 0, LeanAttachment.Alignment.RIGHT);
    }
    return layout;
  }

  public static LeanLayout right(String otherComponent, boolean spanPageWidth) {
    LeanLayout layout = new LeanLayout();
    layout.left = new LeanAttachment(otherComponent, 0, 0, LeanAttachment.Alignment.RIGHT);
    layout.top = new LeanAttachment(otherComponent, 0, 0, LeanAttachment.Alignment.TOP);
    if (spanPageWidth) {
      layout.right = new LeanAttachment(null, 0, 0, LeanAttachment.Alignment.RIGHT);
    }
    return layout;
  }

  public void replaceReferences(String oldName, String newName) {
    for (LeanAttachment attachment : new LeanAttachment[] {left, top, right, bottom}) {
      if (attachment != null && oldName.equals(attachment.getComponentName())) {
        attachment.setComponentName(newName);
      }
    }
  }

  public Set<String> getReferencedLayoutComponentNames() {
    Set<String> names = new HashSet<>();
    for (LeanAttachment attachment : new LeanAttachment[] {left, top, right, bottom}) {
      if (attachment != null && StringUtils.isNotEmpty(attachment.getComponentName())) {
        names.add(attachment.getComponentName());
      }
    }
    return names;
  }

  public boolean hasLeft() {
    return left != null;
  }

  public boolean hasTop() {
    return top != null;
  }

  public boolean hasRight() {
    return right != null;
  }

  public boolean hasBottom() {
    return bottom != null;
  }

  public int numberOfAnchors() {
    int anchors = 0;
    if (hasLeft()) {
      anchors++;
    }
    if (hasRight()) {
      anchors++;
    }
    if (hasTop()) {
      anchors++;
    }
    if (hasBottom()) {
      anchors++;
    }
    return anchors;
  }

  public void validate(LeanComponent component) throws LeanException {
    if (hasLeft()) {
      switch (left.getAlignment()) {
        case TOP:
        case BOTTOM:
          throw new LeanException(
              "Setting a TOP or BOTTOM alignment makes no sense for left attachments on component "
                  + component.getName());
        default:
          break;
      }
    }
    if (hasTop()) {
      switch (top.getAlignment()) {
        case LEFT:
        case RIGHT:
          throw new LeanException(
              "Setting a LEFT or RIGHT alignment makes no sense for top attachments on component "
                  + component.getName());
        default:
          break;
      }
    }
    if (hasRight()) {
      switch (right.getAlignment()) {
        case TOP:
        case BOTTOM:
          throw new LeanException(
              "Setting a TOP or BOTTOM alignment makes no sense for right attachments on component "
                  + component.getName());
        default:
          break;
      }
    }
    if (hasBottom()) {
      switch (bottom.getAlignment()) {
        case LEFT:
        case RIGHT:
          throw new LeanException(
              "Setting a LEFT or RIGHT alignment makes no sense for bottom attachments on component "
                  + component.getName());
        default:
          break;
      }
    }
  }
}
