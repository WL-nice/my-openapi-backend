package com.wanglei.myapiinterface.controller;


import com.wanglei.myapiclientsdk.model.AddNum;
import com.wanglei.myapiclientsdk.model.User;
import com.wanglei.myapiinterface.annotation.AuthCheck;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 名称 API
 */
@Slf4j
@RestController
public class NameController {

    @PostMapping("/name/user")
    @AuthCheck
    public String getUserNamePost(@RequestBody User user,  HttpServletRequest request){
        return "用户名字是: " + user.getUsername();
    }

    @PostMapping("/add/num")
    @AuthCheck
    public String getAddNumPost(@RequestBody AddNum addNum, HttpServletRequest request){
        if(addNum == null || addNum.getNum1() == null || addNum.getNum2() == null){
            return "参数不能为空";
        }
        int sum = addNum.getNum1() + addNum.getNum2();
        return "两数之和是: " + sum;
    }
}
