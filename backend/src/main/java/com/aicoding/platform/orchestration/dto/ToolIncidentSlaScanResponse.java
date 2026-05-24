package com.aicoding.platform.orchestration.dto;

public class ToolIncidentSlaScanResponse {

    private int scanned;
    private int withinSla;
    private int atRisk;
    private int breached;
    private int resolved;

    public ToolIncidentSlaScanResponse() {}

    public int getScanned() { return scanned; }
    public void setScanned(int scanned) { this.scanned = scanned; }

    public int getWithinSla() { return withinSla; }
    public void setWithinSla(int withinSla) { this.withinSla = withinSla; }

    public int getAtRisk() { return atRisk; }
    public void setAtRisk(int atRisk) { this.atRisk = atRisk; }

    public int getBreached() { return breached; }
    public void setBreached(int breached) { this.breached = breached; }

    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }
}
