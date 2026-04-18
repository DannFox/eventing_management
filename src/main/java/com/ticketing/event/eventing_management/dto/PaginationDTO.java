package com.ticketing.event.eventing_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDTO {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private int elementsOnPage;
    private boolean first;
    private boolean last;
    private String sort;
}
