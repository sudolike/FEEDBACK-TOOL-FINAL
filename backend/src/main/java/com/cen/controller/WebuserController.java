package com.cen.controller;

import com.cen.config.MessageeHander;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webuser")
public class WebuserController {
    @GetMapping("/list")
    public Object list(){
        return MessageeHander.SESSION_MAP.keySet();
    }
}
