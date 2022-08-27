package com.umc.footprint.src.walks.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor
public class PostCourseDetailsReq {
    private final String courseName;
    private final String courseImg;
    private final List<List<Double>> coordinates;
    private final List<HashtagVO> hashtags;
    private final String address;
    private final Double length;
    private final Integer courseTime;
    private final Integer walkIdx;
    private final String description;
}
