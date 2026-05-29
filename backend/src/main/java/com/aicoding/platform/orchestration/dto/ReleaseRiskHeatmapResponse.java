package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class ReleaseRiskHeatmapResponse {

    private LocalDate snapshotDate;
    private List<String> categories;
    private List<ReleaseRiskHeatmapCellResponse> cells;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public List<ReleaseRiskHeatmapCellResponse> getCells() { return cells; }
    public void setCells(List<ReleaseRiskHeatmapCellResponse> cells) { this.cells = cells; }
}
