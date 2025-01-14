package titanicsend.pattern.tom;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.*;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEVertex;
import titanicsend.pattern.TEPattern;
import titanicsend.util.PanelStriper;

import java.util.*;
import java.util.stream.Collectors;

public class Pulse extends TEPattern {
    private HashMap<String, LXPoint[][]> pointMap;

    protected final CompoundParameter rate = (CompoundParameter)
            new CompoundParameter("Rate", .25, .01, 2)
                    .setExponent(2)
                    .setUnits(LXParameter.Units.HERTZ)
                    .setDescription("Rate of the rotation");

    protected final SawLFO phase = new SawLFO(0, 1, new FunctionalParameter() {
        public double getValue() {
            return 1000 / rate.getValue();
        }
    });

    public Pulse(LX lx) {
        super(lx);
        startModulator(this.phase);
        addParameter("rate", this.rate);
        pointMap = buildPointMap(model.panelsById);
    }

    public void run(double deltaMs) {
        float phase = this.phase.getValuef();
        for (Map.Entry<String, TEPanelModel> entry : model.panelsById.entrySet()) {
            LXPoint[][] panelPoints = pointMap.get(entry.getKey());
            int litIndex = (int) (phase * (panelPoints.length - 1));
            LXPoint[] litSection = panelPoints[litIndex];

            for (int i = 0; i < entry.getValue().points.length; i++) {
                colors[entry.getValue().points[i].index] = LXColor.BLACK;
            }

            for (LXPoint point : litSection) {
                colors[point.index] = LXColor.WHITE;
            }
        }
    }

    private HashMap<String, LXPoint[][]> buildPointMap(HashMap<String, TEPanelModel> panels) {
        return panels.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> buildPanelMap(e.getValue()),
                        (a, b) -> a,
                        HashMap::new
                ));
    }


    private LXPoint[][] buildPanelMap(TEPanelModel panel) {
        ArrayList<ArrayList<LXPoint>> points = new ArrayList<ArrayList<LXPoint>>();

        TEVertex[] currentVertices = {
                new TEVertex(new LXVector(panel.v0), -1),
                new TEVertex(new LXVector(panel.v1), -1),
                new TEVertex(new LXVector(panel.v2), -1)
        };

        int i = 1;
        while (currentVertices[0].distanceTo(panel.centroid) > (2 * PanelStriper.DISTANCE_BETWEEN_PIXELS)) {
            points.add(0, new ArrayList<LXPoint>());
            LXVector[][] edges = {
                    {currentVertices[0], currentVertices[1]},
                    {currentVertices[1], currentVertices[2]},
                    {currentVertices[0], currentVertices[2]},
            };

            for (LXPoint point : panel.points) {
                for (LXVector[] edge : edges) {
                    if (distanceBetweenPointAndLineSegment(edge[0], edge[1], point) < 2 * PanelStriper.DISTANCE_BETWEEN_PIXELS) {
                        points.get(0).add(point);
                    }
                }
            }

            for (TEVertex vertex : currentVertices) {
                vertex.nudgeToward(panel.centroid, (float) 0.01 * i);
            }
            i++;
        }

        return points.stream()
                .map(l -> l.stream().toArray(LXPoint[]::new))
                .toArray(LXPoint[][]::new);
    }

    private static double distanceBetweenPointAndLineSegment(LXVector line_p1, LXVector line_p2, LXPoint p) {
        LXVector v0 = new LXVector(p);
        LXVector v1 = new LXVector(line_p1);
        LXVector v2 = new LXVector(line_p2);

        double segmentLengthSquared = new LXVector(v2).sub(v1).magSq();
        double t = Math.max(
                0,
                Math.min(1, new LXVector(v0).sub(v1).dot(new LXVector(v2).sub(v1)) / segmentLengthSquared)
        );
        LXVector projection = new LXVector(v1).add(new LXVector(v2).sub(v1).mult((float)t));

        return v0.dist(projection);
    }

}
