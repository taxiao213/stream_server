package com.cn.server.encode;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by hanqq on 2022/6/13
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 * <dependency>
 * <groupId>org.bytedeco</groupId>
 * <artifactId>javacv</artifactId>
 * <version>${javacpp.version}</version>
 * </dependency>
 *
 * <dependency>
 * <groupId>org.bytedeco</groupId>
 * <artifactId>javacv-platform</artifactId>
 * <version>${javacpp.version}</version>
 * </dependency>
 */
public class StreamTest {
    private static Logger logger = LoggerFactory.getLogger(StreamTest.class);

    public static void main(String[] args) throws Exception {
        logger.info("Test main");
//        openCamera();
//        openDeskTop();
//        saveBitmap();
//        preview();
        transcoding2();
//        transcoding();
//        encodeMp4();
    }

    /**
     * 打开桌面
     *
     * @throws Exception
     */
    private static void openDeskTop() throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("desktop");
        grabber.setFormat("gdigrab");
        grabber.setOption("offset_x", "0");
        grabber.setOption("offset_y", "0");
        grabber.setOption("framerate", "25");
        grabber.setOption("draw_mouse", "0");
        grabber.setOption("video_size", "1280x720");
        // 这种形式，双屏有问题
        // grabber.setImageWidth(1920);
        // grabber.setImageWidth(1080);
        grabber.start();
        CanvasFrame canvas = new CanvasFrame("desktop");
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);

        while (canvas.isDisplayable()) {
            canvas.showImage(grabber.grab());
            TimeUnit.MILLISECONDS.sleep(40);
        }

        grabber.stop();
    }

    /**
     * 打开 camera
     *
     * @throws Exception
     */
    private static void openCamera() throws FrameGrabber.Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(1280);
        grabber.setImageHeight(720);
        grabber.start();

        CanvasFrame canvas = new CanvasFrame("摄像头");
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        while (canvas.isDisplayable()) {
            canvas.showImage(grabber.grab());
            TimeUnit.MILLISECONDS.sleep(40);
        }
        grabber.stop();
    }

    /**
     * 生成 h264编码文件
     * @throws FrameRecorder.Exception
     */
    public static void encodeMp4() throws FrameRecorder.Exception {
        String finalVideoPath = "encode.mp4";
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(finalVideoPath, 1024, 768);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        recorder.setFrameRate(25.0);
        recorder.setVideoBitrate((int) ((1024 * 768 * 25) * 2 * 0.07));

        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFormat("mp4");
        recorder.start();

        BufferedImage img = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);

        Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        for (int i = 0; i <= 25 * 20; i++) {
            g2.setColor(Color.white);
            g2.fillRect(0, 0, 1024, 768);
            g2.setPaint(Color.black);
            g2.drawString("frame " + i, 100, 250);
            recorder.record(java2dConverter.convert(img));
        }

        recorder.stop();
        recorder.release();
    }

    /**
     * 保存 bitmap
     *
     * @throws Exception
     */
    public static void saveBitmap() throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("desktop");
        grabber.setFormat("gdigrab");
        grabber.setOption("offset_x", "0");
        grabber.setOption("offset_y", "0");
        grabber.setOption("framerate", "25");
        grabber.setOption("draw_mouse", "0");
        grabber.setOption("video_size", "1280x720");
        // 这种形式，双屏有问题
        // grabber.setImageWidth(1920);
        // grabber.setImageWidth(1080);
        grabber.start();

        Frame frame = grabber.grab();
        Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
        BufferedImage image = java2DFrameConverter.convert(frame);
        if (image != null) {
            File file = new File("desk.jpg");
            ImageIO.write(image, "jpg", file);
        }
    }

    /**
     * 预览视频
     *
     * @throws Exception
     */
    public static void preview() throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("decodeCrash.h264");
        grabber.start();
        CanvasFrame canvasFrame = new CanvasFrame("视频预览");
        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvasFrame.setAlwaysOnTop(true);
        Frame frame;

        Java2DFrameConverter converter = new Java2DFrameConverter();
        while (null != (frame = grabber.grabImage())) {
            canvasFrame.showImage(converter.getBufferedImage(frame));
            Thread.sleep(10);
        }

        grabber.close();
    }

    /**
     * mp4 转 avi
     * 视频转码
     */
    public static void transcoding() throws FrameGrabber.Exception, FrameRecorder.Exception {
        String filePath = "input1.mp4";
        String ext = filePath.substring(filePath.lastIndexOf("."));
        String newFilePath = filePath.replace(ext, "_recode.avi");
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePath);
        grabber.start();
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(newFilePath, grabber.getImageWidth(),
                grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("avi");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(grabber.getFrameRate());
        int bitrate = grabber.getVideoBitrate();
        if (bitrate == 0) {
            bitrate = grabber.getAudioBitrate();
        }
        recorder.setVideoBitrate(bitrate);
        recorder.start(grabber.getFormatContext());

        avcodec.AVPacket packet;
        long dts = 0;
        while ((packet = grabber.grabPacket()) != null) {
            long currentDts = packet.dts();
            if (currentDts >= dts) {
                recorder.recordPacket(packet);
            }
            dts = currentDts;
        }
        recorder.stop();
        recorder.release();
        grabber.stop();
    }

    /**
     * h264 转 mp4
     * @throws FrameGrabber.Exception
     * @throws FrameRecorder.Exception
     */
    public static void transcoding2() throws FrameGrabber.Exception, FrameRecorder.Exception {
        String filePath = "decodeCrash.h264";
        String newFilePath = "decodeCrash.mp4";
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePath);
        grabber.start();
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(newFilePath, grabber.getImageWidth(),
                grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_MP4ALS);
        recorder.setFormat("mp4");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(grabber.getFrameRate());
        int bitrate = grabber.getVideoBitrate();
        if (bitrate == 0) {
            bitrate = grabber.getAudioBitrate();
        }
        recorder.setVideoBitrate(bitrate);
        recorder.start(grabber.getFormatContext());

        avcodec.AVPacket packet;
        long dts = 0;
        while ((packet = grabber.grabPacket()) != null) {
            long currentDts = packet.dts();
            if (currentDts >= dts) {
                recorder.recordPacket(packet);
            }
            dts = currentDts;
        }
        recorder.stop();
        recorder.release();
        grabber.stop();
    }
}
