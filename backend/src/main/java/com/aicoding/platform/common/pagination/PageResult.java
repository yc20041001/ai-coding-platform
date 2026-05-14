package com.aicoding.platform.common.pagination;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {

    private final List<T> records;
    private final Integer page;
    private final Integer pageSize;
    private final Long total;
    private final Boolean hasNext;

    private PageResult(List<T> records, Integer page, Integer pageSize, Long total) {
        this.records = records != null ? records : Collections.emptyList();
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = total != null && (long) page * pageSize < total;
    }

    public static <T> PageResult<T> of(List<T> records, Integer page, Integer pageSize, Long total) {
        return new PageResult<>(records, page, pageSize, total);
    }

    public static <T> PageResult<T> empty(PageQuery pageQuery) {
        return new PageResult<>(Collections.emptyList(), pageQuery.getPage(), pageQuery.getPageSize(), 0L);
    }

    public List<T> getRecords() {
        return records;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public Boolean getHasNext() {
        return hasNext;
    }
}
