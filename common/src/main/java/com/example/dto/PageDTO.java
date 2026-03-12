package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageDTO<T> {
    private int totalPage;
    private long totalRows;
    private int pageNumber;
    private int pageSize;
    private Collection<T> data;
    private boolean last;

    public PageDTO(Page<?> page, Collection<T> data){
        this.totalPage = page.getTotalPages();
        this.totalRows = page.getTotalElements();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.data = data;
        this.last = page.isLast();
    }
}
