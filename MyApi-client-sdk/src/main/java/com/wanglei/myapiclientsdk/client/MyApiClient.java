package com.wanglei.myapiclientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.wanglei.myapiclientsdk.model.AddNum;
import com.wanglei.myapiclientsdk.model.User;
import com.wanglei.myapiclientsdk.utils.SignUtils;


import java.util.HashMap;
import java.util.Map;

/**
 * 调用第三方接口的客户端
 */
public class MyApiClient {
    private final String accessKey;
    private final String secretKet;

//    private static final String GATEWAY_HOST = "http://localhost:8090";//网关端口
    private static final String GATEWAY_HOST = "http://myapi-gateway.wlsite.icu";//网关端口


    public MyApiClient(String accessKey, String secretKet) {
        this.accessKey = accessKey;
        this.secretKet = secretKet;
    }


    private Map<String, String> getHeaderMap(String body) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("nonce", RandomUtil.randomNumbers(5));
        hashMap.put("body", body);
        hashMap.put("accessKey", accessKey);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", SignUtils.getSign(accessKey, secretKet));
        return hashMap;
    }

    public String getUserName(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post(GATEWAY_HOST + "/api/name/user")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        return httpResponse.body();
    }

    public String getAddNum(AddNum addNum) {
        String json = JSONUtil.toJsonStr(addNum);
        HttpResponse httpResponse = HttpRequest.post(GATEWAY_HOST + "/api/add/num")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        return httpResponse.body();
    }

    public String getLoveWord() {
        HttpResponse httpResponse = HttpRequest.get(GATEWAY_HOST + "/api/loveWord")
                .addHeaders(getHeaderMap(null))
                .execute();
        return httpResponse.body();
    }


}
