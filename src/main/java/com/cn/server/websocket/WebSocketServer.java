package com.cn.server.websocket;

import com.cn.server.encode.VideoH264Encoder;
import com.cn.server.encode.Yuv420PGrabber;
import com.cn.server.utils.LogUtils;
import com.google.gson.Gson;
import org.bytedeco.javacpp.avutil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.bytedeco.javacpp.avdevice.avdevice_register_all;
import static org.bytedeco.javacpp.avformat.av_register_all;

/**
 * ws://172.23.0.162:8088/stream_server/websocket
 * stream 转发 服务端
 * Created by taxiao on 2022/6/15
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 * 微信公众号:他晓
 */
@ServerEndpoint(value = "/websocket")
@Component
public class WebSocketServer {

    private Session session;
    private VideoH264Encoder encoder;

    /**
     * 连接建立后触发的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        LogUtils.logger.info("onOpen： " + session.getId());
        WebSocketMapUtil.put(session.getId(), this);
        initDecode();
    }

    private void initDecode() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (encoder == null) {
                    try {
                        int fps = 30;
                        avdevice_register_all();
                        av_register_all();
                        Yuv420PGrabber g = new Yuv420PGrabber();
//                g.open("Integrated Camera", true);
                        // 写绝对路径
//                g.open("C:\\project\\webrtc\\demo\\Webrtc_Java\\input1.mp4", false);
                        g.open("C:\\project\\webrtc\\demo\\stream_server\\time_30_low.mp4", false);
                        encoder = VideoH264Encoder.create(g.getVideoWidth(), g.getVideoHeight(), fps);
                        while (true) {
                            avutil.AVFrame avFrame = g.grab();
                            byte[] buf = encoder.encode(avFrame);
                            if (buf != null) {
                                sendBinary(buf);
                                buf = null;
                                avFrame = null;
                                System.gc();
                            }
                            Thread.sleep(1000 / fps);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    /**
     * 连接关闭后触发的方法
     */

    @OnClose
    public void onClose() {
        //从map中删除
        WebSocketMapUtil.remove(session.getId());
        Set<Map.Entry<String, String>> entries = ClientMapUtil.webSocketMap.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            String value = next.getValue();
            if (StringUtils.pathEquals(session.getId(), value)) {
                iterator.remove();
            }
        }
        LogUtils.logger.info("====== onClose: " + session.getId() + " ======");
    }


    /**
     * 接收到客户端消息时触发的方法
     */

    @OnMessage
    public void onMessage(String params, Session session) {
        //获取服务端到客户端的通道
        WebSocketServer myWebSocket = WebSocketMapUtil.get(session.getId());
        LogUtils.logger.info("收到来自" + session.getId() + "的消息" + params);
        //返回消息给Web Socket客户端（浏览器）
//        myWebSocket.sendMessage(1, "成功", result);
    }

    /**
     * 发生错误时触发的方法
     */

    @OnError
    public void onError(Session session, Throwable error) {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtils.logger.info(session.getId() + "连接发生错误" + error.getMessage());
        error.printStackTrace();
    }

    public void sendMessage(int status, String message, Object datas) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("message", message);
        params.put("datas", datas);
        this.session.getBasicRemote().sendText(new Gson().toJson(params));
    }

    public void sendBinary(byte[] datas) {
        synchronized (this.session) {
            try {
                ByteBuffer wrap = ByteBuffer.wrap(datas);
                if (session.isOpen()) {
                    session.getBasicRemote().sendBinary(wrap);
                }
//        this.session.getAsyncRemote().sendBinary(wrap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage(String message) {
        synchronized (session) {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}