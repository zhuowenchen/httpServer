package tincczw.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.nio.ByteBuffer;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private String remoteHost = "www.sysu.edu.cn";
    private int remotePort = 80;

    private Channel outBoundChannel;
    public HttpServerHandler(){
        super();
    }
    public HttpServerHandler(String remoteHost, int remotePort){
        super();
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if(msg instanceof HttpRequest){
            System.out.println(((HttpRequest) msg).getUri());
            System.out.println(((HttpRequest) msg).getMethod());
            System.out.println(((HttpRequest) msg).getProtocolVersion());
            System.out.println(((HttpRequest) msg).headers().get("Host"));
            System.out.println("收到HTTP请求");
        }
        if(outBoundChannel==null || !ctx.channel().isActive()){
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop())
                    .channel(ctx.channel().getClass())
                    .handler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel ch)
                                throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("codec", new HttpClientCodec());
                            pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
                            pipeline.addLast(new NettyProxyClientHandler(ctx.channel()));
                        }}).option(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.connect(remoteHost,remotePort);
            outBoundChannel = future.channel();
            /* channel建立成功后,将请求发送给远程主机 */


            future.addListener(new ChannelFutureListener(){
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                         URI uri = new URI("/");

            DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,HttpMethod.GET,uri.toASCIIString());
            defaultFullHttpRequest.headers().set(HttpHeaders.Names.HOST,remoteHost);
            defaultFullHttpRequest.headers().set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
                        future.channel().writeAndFlush(defaultFullHttpRequest);
                    } else {
                        future.channel().close();
                    }
                }

            });
        }
       else {
            HttpRequest request = (HttpRequest)msg;
            request.headers().set(HttpHeaders.Names.HOST,remoteHost);
            request.headers().set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
            outBoundChannel.writeAndFlush(request);
        }
    }

    /*@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        DefaultHttpRequest defaultHttpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,HttpMethod.GET,"/");
        ChannelFuture cl = ctx.writeAndFlush(defaultHttpRequest);
    }*/

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }
}
