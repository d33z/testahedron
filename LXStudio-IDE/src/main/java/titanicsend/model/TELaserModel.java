package titanicsend.model;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;

import java.util.ArrayList;
import java.util.List;

public class TELaserModel extends LXModel {
  public LXPoint origin;
  public double elevation;
  public double azimuth;
  public int color;

  // Angles represent the direction the laser is aimed and are in radians, of course.
  public TELaserModel(LXVector v, double elevation, double azimuth) {
    super(makePoint(v));
    this.origin = this.points[0];
    this.elevation = elevation;
    this.azimuth = azimuth;
    this.color = LXColor.rgb(0,0,0);
  }

  private static List<LXPoint> makePoint(LXVector v) {
    List<LXPoint> points = new ArrayList<>();
    points.add(new LXPoint(v));
    return points;
  }
}