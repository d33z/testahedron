package titanicsend.output;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.StreamingACNDatagram;
import titanicsend.model.TEModel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class TESacnOutput {
  private static class SubModelEntry {
    TEModel subModel;
    int universeNum;
    int strandOffset;
    public SubModelEntry(TEModel subModel, int universeNum, int strandOffset) {
      this.subModel = subModel;
      this.universeNum = universeNum;
      this.strandOffset = strandOffset;
    }
  }

  String ipAddress;
  static Map<String, TESacnOutput> ipMap = new HashMap<>();
  private final List<SubModelEntry> subModelEntries;
  private boolean activated;
  private HashMap<Integer,Integer> deviceLengths;

  private TESacnOutput(String ipAddress) {
    this.ipAddress = ipAddress;
    this.subModelEntries = new ArrayList<>();
    this.activated = false;
  }

  public static TESacnOutput getOrMake(String ipAddress) {
    if (!ipMap.containsKey(ipAddress)) {
      ipMap.put(ipAddress, new TESacnOutput(ipAddress));
    }
    return ipMap.get(ipAddress);
  }

  public static void registerSubmodel(TEModel subModel, String ipAddress, int deviceNum, int strandOffset) {
    assert deviceNum >= 1;
    assert deviceNum <= 4;
    assert strandOffset >= 0;
    TESacnOutput output = getOrMake(ipAddress);
    assert !output.activated;
    output.subModelEntries.add(new SubModelEntry(subModel, deviceNum, strandOffset));
  }

  // Sort by device number, then by strand offset
  private static class SortSubModelEntries implements Comparator<SubModelEntry> {
    public int compare(SubModelEntry a, SubModelEntry b) {
      if (a.universeNum != b.universeNum) {
        return a.universeNum - b.universeNum;
      } else {
        return a.strandOffset - b.strandOffset;
      }
    }
  }

  private static void registerOutput(LX lx, InetAddress addr, List<Integer> indexBuffer, int universe) {
    if (indexBuffer.size() == 0) return;
    int[] ib = indexBuffer.stream().mapToInt(i -> i).toArray();
    StreamingACNDatagram outputDevice = new StreamingACNDatagram(lx, ib, universe);
    outputDevice.setAddress(addr);
    lx.addOutput(outputDevice);
  }

  private void activate(LX lx, int gapPointIndex) {
    assert !this.activated;
    this.deviceLengths = new HashMap<>();
    this.subModelEntries.sort(new SortSubModelEntries());
    int currentUniverseNum = 0;
    int currentStrandOffset = -1;

    InetAddress addr;
    try {
      addr = InetAddress.getByName(this.ipAddress);
    } catch (UnknownHostException e) {
      throw new Error(e);
    }

    StringBuilder logString = new StringBuilder("sACN " + this.ipAddress + ": ");
    ArrayList<Integer> indexBuffer = new ArrayList<>();
    for (SubModelEntry subModelEntry : this.subModelEntries) {
      int numPoints = subModelEntry.subModel.points.length;
      if (subModelEntry.universeNum > currentUniverseNum) {
        registerOutput(lx, addr, indexBuffer, currentUniverseNum);
        indexBuffer = new ArrayList<>();
        currentUniverseNum = subModelEntry.universeNum;
        currentStrandOffset = 0;
        String deviceSummary = "#" + currentUniverseNum + " ";
        logString.append(deviceSummary);
      }
      assert subModelEntry.universeNum == currentUniverseNum;

      int gap = subModelEntry.strandOffset - currentStrandOffset;
      if (gap < 0) {
        throw new Error(subModelEntry.subModel.repr() + " offset must be >= " + currentStrandOffset);
      } else if (gap > 0) {
        String gapSummary = "[Gap=" + gap + "] ";
        logString.append(gapSummary);
        currentStrandOffset += gap;
        for (int i = 0; i < gap; i++) indexBuffer.add(gapPointIndex);
      }
      String smSummary = "[" + currentStrandOffset + ":" + subModelEntry.subModel.repr() + "=" + numPoints + "] ";
      logString.append(smSummary);
      currentStrandOffset += numPoints;
      this.deviceLengths.put(currentUniverseNum, currentStrandOffset);
      for (LXPoint point : subModelEntry.subModel.points)
        indexBuffer.add(point.index);
    }

    // We did this in the loop when we changed universes, but there might be one left at the end
    registerOutput(lx, addr, indexBuffer, currentUniverseNum);

    LX.log(logString.toString());
    this.activated = true;
  }

  public static void activateAll(LX lx, int gapPointIndex) {
    List<String> ips = new ArrayList<>(ipMap.keySet());
    Collections.sort(ips);
    for (String ip : ips) {
      ipMap.get(ip).activate(lx, gapPointIndex);
    }
  }
}
