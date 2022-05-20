package com.dp.dto;

import lombok.Data;

import java.util.List;

@Data
public class Feed {
    private List<?> list;
    private Long minTamp;
    private Integer offset;
}
