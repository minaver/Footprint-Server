package com.umc.footprint.src.walks.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
public class GetCourseListRes {
    private int courseIdx;
    private double startLat;
    private double startLong;
    private String courseName;
    private Float courseDist;
    private int courseTime;
    private int courseMark;
    private int courseLike;
    private List<String> courseTags;
    private String courseImg;
    private boolean userCourseLike;

    @Builder
    public GetCourseListRes(int courseIdx, double startLat, double startLong, String courseName, Float courseDist, int courseTime, int courseMark, int courseLike, List<String> courseTags, String courseImg, boolean userCourseLike) {
        this.courseIdx = courseIdx;
        this.startLat = startLat;
        this.startLong = startLong;
        this.courseName = courseName;
        this.courseDist = courseDist;
        this.courseTime = courseTime;
        this.courseMark = courseMark;
        this.courseLike = courseLike;
        this.courseTags = courseTags;
        this.courseImg = courseImg;
        this.userCourseLike = userCourseLike;
    }
}
