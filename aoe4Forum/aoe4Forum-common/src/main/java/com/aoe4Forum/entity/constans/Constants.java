package com.aoe4Forum.entity.constans;

public class Constants {
    public static final String PASSWORD_REGEX = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{6,20}$";

    public final static Integer REDIS_KEY_EXPIRES_ONE_MIN = 60000;

    public static final  Integer REDIS_KEY_EXPIRES_ONE_WEEK = REDIS_KEY_EXPIRES_ONE_MIN*60*24*7;

    public static final Integer TIME_SECOND_WEEK = 60*60*24*7;

    public static final Integer TIME_MILLIS_DAY = REDIS_KEY_EXPIRES_ONE_MIN*60*24;

    public static final Integer POST_HOT_FRESH_TIME = REDIS_KEY_EXPIRES_ONE_MIN;

    public static final String REDIS_KEY_PREFIX = "aoe4Forum:";

    public static  String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX+"checkCode:";

    public static final String REDIS_WEB_TOKEN = REDIS_KEY_PREFIX+"webToken:";

    public static final String WEB_TOKEN = "token";

    public static final String USER_INFO = "userInfo:";

    public static final String USER_FEED = "userFeed:";

    public static final String USER_READ_CURSOR = "userReadCursor:";

    public static final String POST_LIKE_LIST = "postLikeList:";

    public static final String POST_DISLIKE_LIST = "postDislikeList:";

    public static final String COMMENT_LIKE_LIST = "commentLikeList:";

    public static final String NOTICE = "notice:";

    public static final Integer FOLLOW_PAGE_SIZE = 20;

    public static final Integer FEED_PAGE_SIZE = 10;

    public static final Integer NOTICE_MAX_NUM = 60;

    public static final String DEFAULT_AVATAR = "C:\\Users\\81595\\Desktop\\all\\avatarImg\\34.jpg";

    public static final String REDIS_POST_INFO = "post:info:";

    public static final String REDIS_POST_COUNT = "post:count:";

    public static final String REDIS_POST_LIST_FORUM = "post:list:forum:";

    public static final String REDIS_POST_LIST_HOT = "post:list:hot";

    public static final String REDIS_POST_CONTENT = "post:content:";

    public static final String REDIS_FORUM_STATUS = "forum:status:";
}
