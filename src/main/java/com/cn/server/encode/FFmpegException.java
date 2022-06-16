package com.cn.server.encode;

import com.cn.server.utils.LogUtils;

/**
 * Created by hanqq on 2022/6/13
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class FFmpegException extends Exception {

    public FFmpegException(int ret, String st) {
        super(st);
        LogUtils.logger.info(st);
    }

    public FFmpegException(String st) {
        this(0, st);
    }
}
