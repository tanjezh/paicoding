package com.github.paicoding.forum.web.front.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author tanjezh
 * @create 2024-05-12 23:20
 */
@Controller
@RequestMapping("/web/temp")
public class WebTempController {


    @RequestMapping("/index")
    public String temp() {
        return "views/temp/index";

    }
}
