package com.wanglei.myapiclientsdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

/**
 * 签名工具
 */
public class SignUtils {

    public static String getSign(String timestamp,String accessKey, String secretKey) {
        Digester md5 = new Digester(DigestAlgorithm.MD5);
        String content = accessKey + "_" + secretKey+timestamp;
        return md5.digestHex(content);
    }
}
