package com.example.soattro.service;

import com.example.soattro.entity.ClauseType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dựng checklist "hợp đồng này THIẾU gì" (bước 3, code thuần) — tất định.
 *
 * Với mỗi loại điều khoản THIẾT YẾU trong taxonomy: có xuất hiện trong hợp đồng
 * (present=true) hay vắng (false). AI không tham gia — chỉ là phép đếm slot trống.
 */
@Component
public class ChecklistBuilder {

    /**
     * @param presentTypes các loại điều khoản đã bóc được từ hợp đồng
     * @return map loại thiết yếu -> có mặt hay không, theo thứ tự taxonomy (để hiển thị ổn định)
     */
    public Map<ClauseType, Boolean> build(Set<ClauseType> presentTypes) {
        Map<ClauseType, Boolean> result = new LinkedHashMap<>();
        for (ClauseType type : ClauseType.values()) {
            if (type.isEssential()) {
                result.put(type, presentTypes.contains(type));
            }
        }
        return result;
    }
}
