package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.service.ActivityService;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ActivityGraphQlController {

    private final ActivityService activityService;

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

    @GraphQlExceptionHandler
    public GraphQLError notFound(ActivityNotFoundException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(e.getMessage())
                .build();
    }

}









