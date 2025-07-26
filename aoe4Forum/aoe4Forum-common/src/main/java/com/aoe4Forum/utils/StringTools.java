package com.aoe4Forum.utils;

import org.springframework.util.DigestUtils;

public class StringTools {
    public static Boolean isEmpty(String str){
        return str==null||str.isEmpty();
    }
    public static String encodeByMd5(String str){
        return StringTools.isEmpty(str)?null:DigestUtils.md5DigestAsHex(str.getBytes());
    }
}
