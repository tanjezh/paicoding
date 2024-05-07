package com.github.paicoding.forum.service.user.service.help;

import com.github.paicoding.forum.service.user.service.conf.AiConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 星球用户检查
 *
 * @author YiHui
 * @date 2022/12/5
 */
@Component
public class StarNumberHelper {
    @Resource
    private AiConfig aiConfig;

    public Boolean checkStarNumber(String starNumber) {
        // 判断编号是否在 0 - maxStarNumber 之间
        return Integer.parseInt(starNumber) >= 0 && Integer.parseInt(starNumber) <= aiConfig.getMaxNum().getStarNumber();
    }

}
