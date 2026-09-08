package com.veggofresh.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDto {
    private Long totalElements;
    private Long totalPages;
    private Integer size;
    private Integer number;
    private Boolean last;
    private List<NotificationResponseDto> content;
}