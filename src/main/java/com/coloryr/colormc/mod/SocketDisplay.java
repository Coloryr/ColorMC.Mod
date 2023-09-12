package com.coloryr.colormc.mod;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.async.AsyncLogger;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
public class SocketDisplay {
    private boolean done;
    public SocketDisplay() {
        start();
    }

    public void start() {
        String port = System.getProperty("ColorMC.Socket");

        if (port == null) {
            System.out.println("SocketDisplay no port");
            return;
        }

        System.out.println("Start SocketDisplay in " + port);

        try {
            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap boot = new Bootstrap();
            boot.option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .group(group)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new LengthFieldBasedFrameDecoder(2048, 0, 4, 0, 4));
                        }
                    });

            new Thread(() -> {
                try {
                    System.out.println("Start connect " + port);
                    final Channel channel = boot.connect("127.0.0.1", Integer.parseInt(port)).sync().channel();
                    while (!done) {
                        sendData1(channel);
                        ProgressManager.ProgressBar first = null, penult = null, last = null;
                        Iterator<ProgressManager.ProgressBar> i = ProgressManager.barIterator();
                        while (i.hasNext()) {
                            if (first == null) first = i.next();
                            else {
                                penult = last;
                                last = i.next();
                            }
                        }
                        sendBar(channel,1, first);
                        sendBar(channel,2, penult);
                        sendBar(channel,3, last);
                        Thread.sleep(10);
                    }
                    ByteBuf buffer = Unpooled.buffer();
                    buffer.writeInt(5);
                    channel.writeAndFlush(buffer);
                    System.out.println("Disconnect " + port);
                    channel.disconnect();
                } catch (Exception e) {
                    System.out.println("Connect fail");
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            System.out.println("Start connect fail");
            e.printStackTrace();
        }
    }

    private void writeString(ByteBuf buf, String str) {
        if (str.length() > 200) {
            str = str.substring(0, 200);
        }
        byte[] temp = str.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(temp.length).writeBytes(temp);
    }

    private void sendBar(Channel channel, int i, ProgressManager.ProgressBar bar) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(i);
        if (bar == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            writeString(buffer, bar.getTitle());
            writeString(buffer, bar.getMessage());
            buffer.writeInt(bar.getStep()).writeInt(bar.getSteps());
        }
        channel.writeAndFlush(buffer);
    }

    private void sendData1(Channel channel) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(0).writeInt(maxMemory()).writeInt(totalMemory()).writeInt(freeMemory());
        channel.writeAndFlush(buffer);
    }

    private int bytesToMb(long bytes) {
        return (int) (bytes / 1024L / 1024L);
    }

    public int maxMemory() {
        return bytesToMb(Runtime.getRuntime().maxMemory());
    }

    public int totalMemory() {
        return bytesToMb(Runtime.getRuntime().totalMemory());
    }

    public int freeMemory() {
        return bytesToMb(Runtime.getRuntime().freeMemory());
    }

    public void finish() {
        done = true;
    }
}
