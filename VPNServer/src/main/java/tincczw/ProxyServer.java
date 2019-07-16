package tincczw;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import tincczw.handler.HttpServerHandler;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.URI;

public class ProxyServer {

    private NioEventLoopGroup serverWorkerGroup;
    private NioEventLoopGroup serverBossGroup;

    public ProxyServer(){

        serverWorkerGroup = new NioEventLoopGroup();
        serverBossGroup = new NioEventLoopGroup();
    }

    private void init() throws Exception{
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(serverBossGroup,serverWorkerGroup).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(8090)).childHandler(
                    new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                           /* SSLEngine engine =
                            socketChannel.pipeline().addLast("sslhandler",new SslHandler())*/
                           socketChannel.pipeline().addLast("codec",new HttpServerCodec());
                           socketChannel.pipeline().addLast("aggregator",new HttpObjectAggregator(1048567));
                            socketChannel.pipeline().addLast("handler",new HttpServerHandler());
                        }
                    }
            ).option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = serverBootstrap.bind().sync();
           /* URI uri = new URI("http://127.0.0.1:8080");

            DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,HttpMethod.GET,uri.toASCIIString());
            defaultFullHttpRequest.headers().set(HttpHeaders.Names.HOST,"127.0.0.1");
            defaultFullHttpRequest.headers().set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
            f.channel().writeAndFlush(defaultFullHttpRequest);*/

            f.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {

            serverBossGroup.shutdownGracefully();
            serverWorkerGroup.shutdownGracefully();

        }
    }

    public static void main(String[] args) {

        ProxyServer proxyServer = new ProxyServer();
        try{
            proxyServer.init();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
