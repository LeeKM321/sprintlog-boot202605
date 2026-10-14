package com.sprintlog.sprintlogboot.dto.request;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.Visibility;

import java.time.LocalDate;
import java.util.Set;

public record CreateActivityInput(
        ActivityCategory category,
        String title,
        int minutes,
        Visibility visibility,
        Set<String> tags,
        LocalDate studiedOn,
        String instructorName,
        Integer completionRate,
        String bookTitle
) {

    /** REST 와 같은 등록 요청으로 바꾼다 — 검증 규칙(@NotBlank·@Min…)과 등록 로직을 그대로 다시 쓰기 위해서. */
    public CreateActivityRequest toRequest() {
        return new CreateActivityRequest(category, title, minutes, visibility, tags, studiedOn,
                instructorName, completionRate, bookTitle);
    }
}
