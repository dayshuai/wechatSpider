package com.dayshuai.wechat.controller;

import com.dayshuai.wechat.dto.Response;
import com.dayshuai.wechat.utils.WechatAriticleCrawl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private WechatAriticleCrawl wechatCraw;


    @GetMapping
    public Response<Map<String, Object>> get(){
        Response<Map<String, Object>> response = new Response<>();
        Map<String, Object> user = new HashMap<>();
        wechatCraw.run();
        user.put("name", "demo");
        user.put("age", 25);
        response.setData(user);
        return  response;
    }
}