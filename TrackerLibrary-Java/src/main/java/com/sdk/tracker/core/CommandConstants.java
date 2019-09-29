package com.sdk.tracker.core;


public class CommandConstants {
    public static final String GET_LOG = "getLog";
    //获取结果
    public static final String GET_RESULT = "getResult";
    //方法的参数
    public static final String GET_ARGS = "getArgs";
    //类的属性
    public static final String GET_PROP = "getProp";
    //获取日志指令
    public static final String GET_LOG_BEFORE_COMMAND = "before#getLog#";

    /**
     * 最小的日志数达到后上传
     */
    public static final int MIN_COUNT_UPLOAD = 10;
    /**
     * 某方法最多记录次数
     */
    public static final int MAX_METHOD_RECORD = 3;
}
