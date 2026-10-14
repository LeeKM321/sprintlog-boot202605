package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.User;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityInput;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.service.ActivityService;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ActivityGraphQlController {

    private final ActivityService activityService;
    private final Validator validator;

    @QueryMapping
    public List<LearningActivity> activities(@Argument ActivityCategory category,
                                             @Argument String keyword,
                                             @Argument Integer minMinutes) {
        return activityService.searchActivities(category, keyword, minMinutes);
    }

    @QueryMapping
    public LearningActivity activity(@Argument Long id) {
        return activityService.get(id);
    }

    @BatchMapping(typeName = "Activity")
    public Map<LearningActivity, User> owner(List<LearningActivity> activities) {
        log.info("[owner] 활동 {}개의 주인을 한번에 찾는다", activities.size());
        return activityService.ownersOf(activities);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public LearningActivity createActivity(@Argument CreateActivityInput input, Authentication authentication) {
        CreateActivityRequest request = input.toRequest();
        Set<ConstraintViolation<CreateActivityRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return activityService.create(request, null, authentication.getName());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public LearningActivity updateActivity(@Argument Long id, @Argument @Valid UpdateActivityRequest input) {
        return activityService.update(id, input);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteActivity(@Argument Long id) {
        activityService.delete(id);
        return true;
    }

    @GraphQlExceptionHandler
    public GraphQLError notFound(ActivityNotFoundException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(e.getMessage())
                .build();
    }

}









