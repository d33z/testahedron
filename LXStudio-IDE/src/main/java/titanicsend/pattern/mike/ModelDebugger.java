package titanicsend.pattern.mike;

import java.util.*;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p4lx.ui.UI2dComponent;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UITextBox;
import titanicsend.pattern.TEPattern;

public class ModelDebugger extends TEPattern implements UIDeviceControls<ModelDebugger> {
  public enum ObjectType {
    VERTEX("Vertex"),
    EDGE("Edge"),
    PANEL("Panel"),
    LASER("Laser");

    public final String label;

    ObjectType(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final EnumParameter<ObjectType> objectType =
          new EnumParameter<ObjectType>("Object Type", ObjectType.VERTEX)
                  .setDescription("Which type of object to light up");

  public final DiscreteParameter pointIndex =
          new DiscreteParameter("Index", -1, 1000)
                  .setDescription("Pixel within the selected object to light up (-1 for all)");

  public final StringParameter objectId =
          new StringParameter("ID")
                  .setDescription("ID of the object to light up (blank for all)");

  private UI2dComponent idErrLabel;
  private UI2dComponent pointErrLabel;

  public ModelDebugger(LX lx) {
    super(lx);
  }

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, ModelDebugger pattern) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
    uiDevice.setChildSpacing(6);
    uiDevice.setContentWidth(COL_WIDTH);

    UITextBox tb;

    uiDevice.addChildren(
            newDropMenu(objectType),
            controlLabel(ui, "ID"),
            tb = new UITextBox(0, 0, COL_WIDTH, 16).setParameter(objectId),
            controlLabel(ui, "Point"),
            newIntegerBox(pointIndex),
            this.idErrLabel = controlLabel(ui, "Bad ID"),
            this.pointErrLabel = controlLabel(ui, "Bad point")
    );

    tb.setEmptyValueAllowed(true);

    this.objectType.addListener(this::repaint);
    this.objectId.addListener(this::repaint);
    this.pointIndex.addListener(this::repaint);
  }

  public void repaint(LXParameter unused) {
    this.clearPixels();
    List<LXModel> subModels = new ArrayList<>();
    String idStr = this.objectId.getString().trim().toUpperCase();
    boolean getAll = idStr.equals("");

    this.idErrLabel.setVisible(false);

    switch (this.objectType.getEnum()) {
      case VERTEX:
        // TODO: implement
        break;
      case EDGE:
        if (getAll)
          subModels.addAll(this.model.edgesById.values());
        else if (this.model.edgesById.containsKey(idStr))
          subModels.add(this.model.edgesById.get(idStr));
        else
          this.idErrLabel.setVisible(true);
        break;
      case PANEL:
        if (getAll)
          subModels.addAll(this.model.panelsById.values());
        else if (this.model.panelsById.containsKey(idStr))
          subModels.add(this.model.panelsById.get(idStr));
        else
          this.idErrLabel.setVisible(true);
        break;
      case LASER:
        // TODO: Implement
        break;
      default:
          throw new Error("huh?");
    }

    // If no submodels, don't print error about invalid points.
    // If there are submodels, turn on the error for now and see if it gets turned off.
    boolean pointErr = !subModels.isEmpty();

    int pi = this.pointIndex.getValuei();
    for (LXModel subModel : subModels) {
      for (int i = 0; i < subModel.points.length; i++) {
        if (pi < 0 || pi == i) {
          pointErr = false;
          LXPoint point = subModel.points[i];
          colors[point.index] = LXColor.WHITE;
        }
      }
    }
    this.pointErrLabel.setVisible(pointErr);
  }

  public void run(double deltaMs) {
  }
}