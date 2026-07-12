package com.example.soattro.dto.response;

import com.example.soattro.entity.ChecklistItem;
import com.example.soattro.entity.ClauseType;

/** Một dòng checklist "hợp đồng THIẾU gì" cho frontend. */
public record ChecklistItemDto(String clauseType, String label, boolean present) {

    public static ChecklistItemDto from(ChecklistItem item) {
        return new ChecklistItemDto(
                item.getClauseType().name(),
                item.getClauseType().getLabel(),
                item.isPresent());
    }

    public static ChecklistItemDto of(ClauseType type, boolean present) {
        return new ChecklistItemDto(type.name(), type.getLabel(), present);
    }
}
