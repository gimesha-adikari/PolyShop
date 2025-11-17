package com.polyshop.common.paging;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageResponse<T> {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<T> content;

    public PageResponse() {
    }

    public PageResponse(int page, int size, long totalElements, int totalPages, List<T> content) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.content = content;
    }

    public static <T> PageResponse<T> of(
            int page, int size, long totalElements, List<T> content) {

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(page, size, totalElements, totalPages, content);
    }
}
