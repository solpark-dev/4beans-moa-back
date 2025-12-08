package com.moa.dto.community.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    private List<T> content;
    private int page;
    private int size;
    private long total;
    
    public int getTotalPages() {
        return (int) Math.ceil((double) total / size);
    }
    
    public boolean hasNext() {
        return page < getTotalPages();
    }
    
    public boolean hasPrevious() {
        return page > 1;
    }
}