package com.umc.footprint.src.users;




import com.umc.footprint.src.users.model.GetUserTodayRes;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.umc.footprint.src.users.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;;
import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponse;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private final UserProvider userProvider;
    @Autowired
    private final UserService userService;

    public UserController(UserProvider userProvider, UserService userService) {
        this.userProvider = userProvider;
        this.userService = userService;
    }


    @GetMapping("/{useridx}/today")
    public BaseResponse<List<GetUserTodayRes>> getToday(@PathVariable("useridx") int userIdx){
        try{
            List<GetUserTodayRes> userTodayRes = userProvider.getUserToday(userIdx);

            return new BaseResponse<>(userTodayRes);
        } catch (BaseException exception){
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /**
     * 유저 정보 조회 API
     * [GET] /users/:userIdx
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/{userIdx}") // (GET) 127.0.0.1:3000/users/:userIdx
    public BaseResponse<GetUserRes> getUser(@PathVariable("userIdx") int userIdx) {
        try {
            GetUserRes getUserRes = userProvider.getUser(userIdx);
            return new BaseResponse<>(getUserRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }

    /**
     * 유저 "이번달" 목표 조회 API
     * [GET] /users/:userIdx/goals
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/{userIdx}/goals") // (GET) 127.0.0.1:3000/users/:userIdx/goals
    public BaseResponse<GetUserGoalRes> getUserGoal(@PathVariable("userIdx") int userIdx) {
        try {
            GetUserGoalRes getUserGoalRes = userProvider.getUserGoal(userIdx);
            return new BaseResponse<>(getUserGoalRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }

    /**
     * 유저 "다음달" 목표 조회 API
     * [GET] /users/:userIdx/goals/next
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/{userIdx}/goals/next") // (GET) 127.0.0.1:3000/users/:userIdx/goals/next
    public BaseResponse<GetUserGoalRes> getUserGoalNext(@PathVariable("userIdx") int userIdx) {
        try {
            GetUserGoalRes getUserGoalRes = userProvider.getUserGoalNext(userIdx);
            return new BaseResponse<>(getUserGoalRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }


}
