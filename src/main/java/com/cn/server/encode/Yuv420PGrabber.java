package com.cn.server.encode;


import org.bytedeco.javacpp.avcodec.*;
import org.bytedeco.javacpp.avformat.*;
import org.bytedeco.javacpp.avutil.*;

import java.io.FileOutputStream;
import java.io.OutputStream;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avdevice.avdevice_register_all;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

/**
 * @author itqn
 */
public class Yuv420PGrabber {

    // 格式对象
    private AVFormatContext pFormatCtx;
    // 解码器上下文
    private AVCodecContext pCodecCtx;
    // 解码器
    private AVCodec pCodec;
    // 采集帧
    private AVFrame pFrame;
    // 包
    private AVPacket pkt;
    // 视频流索引
    private int videoIdx = -1;
    private int videoWidth;
    private int videoHeight;
    // 视频帧转化器
//	private VideoConverter videoConverter=null;

    // 临时变量
    private int ret = 0;
    private int[] got = {0};

    /**
     * 打开视频流
     *
     * @param input
     */
    public void open(String input, boolean openCamera) throws FFmpegException {
        av_register_all();
        avdevice_register_all();
        pFormatCtx = avformat_alloc_context();
//		打开摄像头
        if (openCamera) {
            ret = avformat_open_input(pFormatCtx, String.format("video=%s", input), av_find_input_format("dshow"),
                    (AVDictionary) null);
        } else {
            ret = avformat_open_input(pFormatCtx, input, null, (AVDictionary) null);
        }

        if (ret != 0) {
            throw new FFmpegException(ret, "avformat_open_input 打开视频流失败");
        }
        ret = avformat_find_stream_info(pFormatCtx, (AVDictionary) null);
        if (ret < 0) {
            throw new FFmpegException(ret, "avformat_find_stream_info 查找视频流失败");
        }
        for (int i = 0; i < pFormatCtx.nb_streams(); i++) {
            if (pFormatCtx.streams(i).codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
                videoIdx = i;
                break;
            }
        }
        if (videoIdx == -1) {
            throw new FFmpegException("没有找到视频流");
        }

        // 初始化解码器
        pCodecCtx = pFormatCtx.streams(videoIdx).codec();
        pCodec = avcodec_find_decoder(pCodecCtx.codec_id());
        if (pCodec == null) {
            throw new FFmpegException("没有找到合适的解码器：" + pCodecCtx.codec_id());
        }
        // 打开解码器
        ret = avcodec_open2(pCodecCtx, pCodec, (AVDictionary) null);
        if (ret != 0) {
            throw new FFmpegException(ret, "avcodec_open2 解码器打开失败");
        }

        videoWidth = pCodecCtx.width();
        videoHeight = pCodecCtx.height();
        // 创建帧、包
        pFrame = av_frame_alloc();
        pkt = new AVPacket();

        // 初始化帧转化器
//		videoConverter = VideoConverter.create(videoWidth, videoHeight, pCodecCtx.pix_fmt(), videoWidth, videoHeight, //
//				AV_PIX_FMT_YUV420P);
        // size
        System.out.println(pCodecCtx.width() + "x" + pCodecCtx.height());
    }

    /**
     * 采集一帧视频（YUV格式）
     */
    public AVFrame grab() throws FFmpegException {
        if (av_read_frame(pFormatCtx, pkt) >= 0 && pkt.stream_index() == videoIdx) {
            ret = avcodec_decode_video2(pCodecCtx, pFrame, got, pkt);
            if (ret < 0) {
                throw new FFmpegException(ret, "avcodec_decode_video2 解码失败");
            }
            if (got[0] != 0) {
//				return videoConverter.scale(pFrame);
                return pFrame;
            }
            av_packet_unref(pkt);
        }
        return null;
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (pkt != null) {
            av_free_packet(pkt);
            pkt = null;
        }
        if (pFrame != null) {
            av_frame_free(pFrame);
            pFrame = null;
        }
//		if (videoConverter != null) {
//			videoConverter.release();
//		}
        if (pCodecCtx != null) {
            avcodec_free_context(pCodecCtx);
            pCodecCtx = null;
        }
        if (pFormatCtx != null) {
            avformat_close_input(pFormatCtx);
            pFormatCtx = null;
        }
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }


    public static void main(String[] args) throws Exception {
        Yuv420PGrabber g = new Yuv420PGrabber();
        int fps = 25;
        g.open("Integrated Camera", true);
        byte[] y = new byte[g.getVideoWidth() * g.getVideoHeight()];
        byte[] u = new byte[g.getVideoWidth() * g.getVideoHeight() / 4];
        byte[] v = new byte[g.getVideoWidth() * g.getVideoHeight() / 4];
        //  1280x720
        OutputStream fos = new FileOutputStream("yuv420p.yuv");
        for (int i = 0; i < 200; i++) {
            AVFrame avFrame = g.grab();
            avFrame.data(0).get(y);
            avFrame.data(1).get(u);
            avFrame.data(2).get(v);
            fos.write(y);
            fos.write(u);
            fos.write(v);
            Thread.sleep(1000 / fps);
        }
        fos.flush();
        fos.close();

        g.close();
    }
}


