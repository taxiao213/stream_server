package com.cn.server.encode;

import org.bytedeco.javacpp.avcodec.*;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avdevice.avdevice_register_all;
import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;

/**
 * H264编码器
 *
 * @author itqn
 */
public class VideoH264Encoder {
    // 视频编码器
    private AVCodecContext pCodecCtx;
    // 视频编码
    private AVCodec pCodec;
    // 编码后的包
    private AVPacket pkt;
    // 临时变量
    private int ret = 0;
    private int[] got = {0};

    // no instance
    private VideoH264Encoder() {
    }

    /**
     * 创建编码器
     *
     * @param width
     * @param height
     * @param fps
     */
    public static VideoH264Encoder create(int width, int height, int fps) throws FFmpegException {
        return create(width, height, fps, new HashMap<>());
    }

    /**
     * 创建编码器
     *
     * @param width
     * @param height
     * @param fps
     * @param opts
     */
    public static VideoH264Encoder create(int width, int height, int fps, Map<String, String> opts)
            throws FFmpegException {
        VideoH264Encoder h = new VideoH264Encoder();
        // 查找H264编码器
        h.pCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
        if (h.pCodec == null) {
            throw new FFmpegException("初始化 AV_CODEC_ID_H264 编码器失败");
        }
        // 初始化编码器信息
        h.pCodecCtx = avcodec_alloc_context3(h.pCodec);
        h.pCodecCtx.codec_id(AV_CODEC_ID_H264);
        h.pCodecCtx.codec_type(AVMEDIA_TYPE_VIDEO);
        h.pCodecCtx.pix_fmt(AV_PIX_FMT_YUV420P);
        h.pCodecCtx.width(width);
        h.pCodecCtx.height(height);
        h.pCodecCtx.time_base().num(1);
        h.pCodecCtx.time_base().den(fps);
        // 其他参数设置
        AVDictionary dictionary = new AVDictionary();
        opts.forEach((k, v) -> {
            avutil.av_dict_set(dictionary, k, v, 0);
        });
        h.ret = avcodec_open2(h.pCodecCtx, h.pCodec, dictionary);
        if (h.ret < 0) {
            throw new FFmpegException(h.ret, "avcodec_open2 编码器打开失败");
        }
        h.pkt = new AVPacket();
        return h;
    }

    /**
     * 编码
     *
     * @param avFrame
     * @return
     */
    public byte[] encode(AVFrame avFrame) throws FFmpegException {
        if (avFrame == null) {
            return null;
        }
        byte[] bf = null;
        try {
            avFrame.format(pCodecCtx.pix_fmt());
            avFrame.width(pCodecCtx.width());
            avFrame.height(pCodecCtx.height());
            ret = avcodec_encode_video2(pCodecCtx, pkt, avFrame, got);
            if (ret < 0) {
                throw new FFmpegException(ret, "avcodec_encode_video2 编码失败");
            }
            if (got[0] != 0) {
                bf = new byte[pkt.size()];
                pkt.data().get(bf);
            }
            av_packet_unref(pkt);
        } catch (Exception e) {
            throw new FFmpegException(e.getMessage());
        }
        return bf;
    }

    /**
     * 关闭编码器
     */
    public void release() {
        if (pkt != null) {
            av_free_packet(pkt);
            pkt = null;
        }
        if (pCodecCtx != null) {
            avcodec_free_context(pCodecCtx);
            pCodecCtx = null;
        }
    }

    public static void main(String[] args) throws Exception {
        int fps = 25;
        avdevice_register_all();
        av_register_all();

        Yuv420PGrabber g = new Yuv420PGrabber();
        g.open("time.flv", false);
//        g.open("Integrated Camera", true);
        VideoH264Encoder encoder = VideoH264Encoder.create(g.getVideoWidth(), g.getVideoHeight(), fps);
        OutputStream fos = new FileOutputStream("inputq.h264");
        for (int i = 0; i < 200; i++) {
            AVFrame avFrame = g.grab();
            byte[] buf = encoder.encode(avFrame);
            if (buf != null) {
                fos.write(buf);
            }
            Thread.sleep(1000 / fps);

        }
		fos.flush();
		fos.close();
        encoder.release();
        g.close();
    }
}
