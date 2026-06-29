package com.sprintlog.sprintlogboot.service;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.dto.response.ActivityResponse;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository repository;


    public List<ActivityResponse> search(ActivityCategory category, String keyword, Integer minMinutes) {

        if (category != null) {
           return convertToDtoList(repository.findByCategory(category));
        }
        if (keyword != null && !keyword.isBlank()) {
            return convertToDtoList(repository.findByTitleContainingIgnoreCase(keyword));
        }
        if (minMinutes != null) {
            return convertToDtoList(repository.findLongActivities(minMinutes));
        }

        return convertToDtoList(repository.findAll());
    }


    private List<ActivityResponse> convertToDtoList(List<LearningActivity> list) {
        return list.stream()
                .map(a -> ActivityResponse.from(a))
                .toList();
    }


    @Transactional
    public void deleteByTitleAndCategory(String title, ActivityCategory category) {
        repository.deleteByTitleAndCategoryWithJPQL(title, category);
    }
}














