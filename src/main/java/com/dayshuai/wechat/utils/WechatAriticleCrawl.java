package com.dayshuai.wechat.utils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;


@Component
public class WechatAriticleCrawl {
    private final static Logger logger = LoggerFactory.getLogger(WechatAriticleCrawl.class);

    public static String USER_AGENT = "User-Agent";
    public static String USER_AGENT_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36";
    public static String COOKIE = "Cookie";
    static Map<String, String> COOKIES = new HashMap<String, String>();
    public static String URL = "https://weixin.sogou.com/weixin?type=1&s_from=input&query=${chatNo}&ie=utf8&_sug_=y&_sug_type_=&w=01019900&sut=9970&sst0=1591664819492&lkt=0%2C0%2C0";
    public static String BASEURL = "https://weixin.sogou.com";
    public static String HOMEURL = "http://www.sogou.com/web?ie=utf8&query=jzfx01";
    @Value("${wechatIds}")
    private String wechatIds;

    @Autowired
    SendEmailUtils sendEmailUtils;

    /**
     * key:wechatid
     * value:title
     */
    private Map<String, String> wechatIdTitleMap = new HashMap<>();


    /**
     * key:wechatid
     * value:emailSendedTile
     */
    private Map<String, List<String>> emailSendedTileMap = new HashMap<>();

    /**
     * 执行任务开始
     */
    public void run() {
        String [] wechatNoArr = wechatIds.split(",");
        for (String wechatNo : wechatNoArr ) {
            hasUpdated(wechatNo);
        }
    }


    private void hasUpdated(String wechatNo) {
        try {
            //先进入搜狗获取header、cookie、URl
            String cookies = getHeaderCookie(wechatNo);
            //进入公众号页面
            String topOneTitle = requestInvoke(cookies, wechatNo);
            // 与缓存不同时发邮件
            String memoryTitle = wechatIdTitleMap.get(wechatNo);
            logger.info("topOneTitle：{}， memoryTitle：{}", topOneTitle, memoryTitle);
            //网页中的标题不为空时对比
            if (StringUtils.isNotBlank(topOneTitle) && !StringUtils.equals(memoryTitle, topOneTitle)) {
                try {
                    //通知处理
                    handleNotify(wechatNo, topOneTitle);
                    wechatIdTitleMap.put(wechatNo, topOneTitle);
                } catch (Exception e) {
                    logger.error("error:{}", e);
                }
            }

        } catch (IOException e) {
            logger.error("error:{}", e);
        }
    }

    private void handleNotify(String wechatNo, String topOneTitle) throws Exception {
        //同一个标题只发一次邮件
        List<String> titleList = emailSendedTileMap.get(wechatNo);
        //如果为空，则第一次爬虫，加入标题
        if (titleList == null ) {
            logger.info("---開始發送郵件---");
            sendEmailUtils.sendTextEmail(sendEmailUtils.to, wechatNo, topOneTitle);
            titleList = new ArrayList<>();
            titleList.add(topOneTitle);
            emailSendedTileMap.put(wechatNo,titleList);
        } else if (!titleList.contains(topOneTitle)) {
            sendEmailUtils.sendTextEmail(sendEmailUtils.to, wechatNo, topOneTitle);
            if (titleList.size() > 10) {
                titleList.clear();
            }
            titleList.add(topOneTitle);
            emailSendedTileMap.put(wechatNo,titleList);
        }
    }

    /**
     * 请求url返回标题
     *
     * @param cookies
     * @return
     * @throws IOException
     */
    private static String requestInvoke(String cookies, String wechatNo) throws IOException {
        String requestUrl = URL.replace("${chatNo}", wechatNo);
        logger.info("请求公众号地址：{}", requestUrl);
        Connection con = Jsoup.connect(requestUrl).header(USER_AGENT, USER_AGENT_VALUE).header(COOKIE, cookies); // 获取connection
        Response rs = con.execute();
        Document dodetail = Jsoup.parse(rs.body());
        Elements AElement = dodetail.select("dd > a");
        logger.info("标题dom：" + AElement.toString());
        String topOne = "";
        if (AElement != null) {
            Element topOneEle = AElement.get(0);
            topOne = topOneEle.text();
        }
        logger.info("标题：" + topOne);
        String articleUrl = BASEURL + AElement.attr("href");
        logger.info("文章url：" + articleUrl);
        return topOne;
    }

    /**
     * 获取header、cookie、URl
     *
     * @param wechatNo
     * @return
     * @throws IOException
     */
    private static String getHeaderCookie(String wechatNo) throws IOException {


        Connection baseCon = Jsoup.connect(HOMEURL); // 获取connection
        baseCon.header(USER_AGENT, USER_AGENT_VALUE); // 配置模拟浏览器
        Response baseRs = baseCon.execute(); // 获取响应
        // Document doBase = Jsoup.parse(baseRs.body());
        //获取cookie
        String cookies = baseRs.cookies().toString().replace("{", "").replace("}", "").replace(",", ";");
        // 生成16位的随机数
        String time = String.valueOf(System.currentTimeMillis()) + getRandomString(3) + ";";
        cookies += ";SUV=" + time;
        return cookies;
    }


    /**
     * 获取随机数
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        String str = "0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    /**
     * @return
     */
    public static String getGUID() {
        StringBuilder uid = new StringBuilder();
        // 产生16位的强随机数
        Random rd = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            // 产生0-2的3位随机数
            int type = rd.nextInt(3);
            switch (type) {
                case 0:
                    // 0-9的随机数
                    uid.append(rd.nextInt(10));
                    /*
                     * int random = ThreadLocalRandom.current().ints(0, 10)
                     * .distinct().limit(1).findFirst().getAsInt();
                     */
                    break;
                case 1:
                    // ASCII在65-90之间为大写,获取大写随机
                    uid.append((char) (rd.nextInt(25) + 65));
                    break;
                case 2:
                    // ASCII在97-122之间为小写，获取小写随机
                    uid.append((char) (rd.nextInt(25) + 97));
                    break;
                default:
                    break;
            }
        }
        return uid.toString();
    }

        static int arr[] = new int[5];
    public static void main(String[] args) {



        System.out.println(AccountType.FIXED);
    }


    enum AccountType
    {
        SAVING, FIXED, CURRENT;
        private AccountType()
        {
            System.out.println("11");
        }
    }


}
