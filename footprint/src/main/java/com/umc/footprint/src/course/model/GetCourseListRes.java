package com.umc.footprint.src.course.model;

import com.umc.footprint.src.model.Course;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@NoArgsConstructor
public class GetCourseListRes implements Comparable<GetCourseListRes>{
    private int courseIdx;
    private double startLat;
    private double startLong;
    private String courseName;
    private Double courseDist;
    private int courseTime;
    private int courseCount;
    private int courseLike;
    private List<String> courseTags;
    private String courseImg;
    private boolean userCourseMark;

    @Builder
    public GetCourseListRes(int courseIdx, double startLat, double startLong, String courseName, Double courseDist,
                            int courseTime, int courseCount, int courseLike, List<String> courseTags, String courseImg,
                            boolean userCourseMark) {
        this.courseIdx = courseIdx;
        this.startLat = startLat;
        this.startLong = startLong;
        this.courseName = courseName;
        this.courseDist = courseDist;
        this.courseTime = courseTime;
        this.courseCount = courseCount;
        this.courseLike = courseLike;
        this.courseTags = courseTags;
        this.courseImg = courseImg;
        this.userCourseMark = userCourseMark;
    }

    public static GetCourseListRes of(Course course, int courseCount, String courseImgUrl, List<String> courseTags, boolean isMark) {
        return GetCourseListRes.builder()
                .courseIdx(course.getCourseIdx())
                .startLat(course.getStartCoordinate().getX())
                .startLong(course.getStartCoordinate().getY())
                .courseName(course.getCourseName())
                .courseDist(course.getLength())
                .courseTime(course.getCourseTime())
                .courseCount(courseCount)
                .courseLike(course.getLikeNum())
                .courseImg(courseImgUrl)
                .courseTags(courseTags)
                .userCourseMark(isMark)
                .build();
    }

    @Override
    public int compareTo(GetCourseListRes getCourseListRes){
        if(getCourseListRes.courseDist < courseDist){
            return 1;
        } else if(getCourseListRes.courseDist > courseDist){
            return -1;
        }
        return 0;
    }
}
