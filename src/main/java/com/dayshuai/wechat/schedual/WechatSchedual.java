package com.dayshuai.wechat.schedual;

import com.dayshuai.wechat.utils.WechatAriticleCrawl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WechatSchedual {

    @Autowired
    private WechatAriticleCrawl wechatCraw;


    @Scheduled(cron = "${wechatCron}")
    public void doSpider () {
        wechatCraw.run();
    }

    @Scheduled(cron = "${wechatCronAnother}")
    public void doSpiderAnother () {
        wechatCraw.run();
    }
}
