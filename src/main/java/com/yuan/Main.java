package com.yuan;

import com.yuan.util.Session;
import com.yuan.util.SessionBuilder;
import com.yuan.util.SessionException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
/**
 * @auhtor yswit@outlook.com
 * @create 2018-07-30 下午9:13
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Map.class);

    List<String> urlList = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws SessionException, IOException {

        Session session = new SessionBuilder().build();

        String journalource = "中国药师";
        String yearStart = "2014";
        String yearEnd = "2018";

        new Main().doQuery(session, journalource, yearStart, yearEnd);
    }


    void doQuery(Session session, String magazineValue, String yaerFrom, String yearTo) throws SessionException, IOException {

        //请求查询参数
        final String url1 = "http://kns.cnki.net/kns/request/SearchHandler.ashx?action=&NaviCode=*&ua=1.21&PageName=ASP.brief_result_aspx&DbPrefix=CJFQ&DbCatalog=%e4%b8%ad%e5%9b%bd%e5%ad%a6%e6%9c%af%e6%9c%9f%e5%88%8a%e7%bd%91%e7%bb%9c%e5%87%ba%e7%89%88%e6%80%bb%e5%ba%93&ConfigFile=CJFQ.xml&db_opt=CJFQ&db_value=%E4%B8%AD%E5%9B%BD%E5%AD%A6%E6%9C%AF%E6%9C%9F%E5%88%8A%E7%BD%91%E7%BB%9C%E5%87%BA%E7%89%88%E6%80%BB%E5%BA%93&" +
                "magazine_value1=" + magazineValue + "&magazine_special1=%25&" +
                "year_from=" + yaerFrom + "&" +
                "year_to=" + yearTo + "&year_type=echar&his=0&__=Mon%20Jul%2030%202018%2021%3A25%3A40%20GMT%2B0800%20(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)";

        HttpResponse response = session.next(url1).get(1);
        EntityUtils.toString(response.getEntity());


        //请求第一页数据
        final String url2 = "http://kns.cnki.net/kns/brief/brief.aspx?pagename=ASP.brief_result_aspx&dbPrefix=CJFQ&dbCatalog=%e4%b8%ad%e5%9b%bd%e5%ad%a6%e6%9c%af%e6%9c%9f%e5%88%8a%e7%bd%91%e7%bb%9c%e5%87%ba%e7%89%88%e6%80%bb%e5%ba%93&ConfigFile=CJFQ.xml&research=off&t=1532956853014&keyValue=&S=1";

        response = session.next(url2).post(1);

        String res = EntityUtils.toString(response.getEntity());

        Document document = Jsoup.parse(res);

        //第一页下载链接
        logger.info("---------解析第一页下载链接...");
        parseDownlodUrl(res);


        //总页数
        String pageNumText = document.select("#J_ORDER > tbody > tr:nth-child(2) > td > table > tbody > tr > td:nth-child(2) > div > span.countPageMark").text();

        if (!pageNumText.contains("/")) {
            logger.error("解析总页数异常, text: {}", pageNumText);
            return;
        }

        int pageTotal = Integer.valueOf(pageNumText.substring(pageNumText.indexOf("/") + 1));
        logger.info("总页数:{}", pageTotal);

        for (int i = 2; i <= pageTotal; i++) {

            logger.info("---------解析第{}页下载链接...", i);
            String url3 = "http://kns.cnki.net/kns/brief/brief.aspx?" +
                    "curpage=" + i + "&RecordsPerPage=20&QueryID=4&ID=&turnpage=1&tpagemode=L&dbPrefix=CJFQ&Fields=&DisplayMode=listmode&PageName=ASP.brief_result_aspx";

            HttpResponse httpResponse = session.next(url3).get(1);

            res = EntityUtils.toString(httpResponse.getEntity());
            parseDownlodUrl(res);
        }

        System.out.println();
    }

    private void parseDownlodUrl(String html) {

        Document document = Jsoup.parse(html);

        Elements trs = document.select("#ctl00 > table > tbody > tr:nth-child(2) > td > table > tbody > tr");

        trs.stream()
                .filter(tr -> !tr.attr("bgcolor").equals(""))
                .map(tr -> {
                    return tr.select(" td:nth-child(7) > a").attr("href");
                }).forEach(url -> {
            urlList.add(url);
        });
    }
}
