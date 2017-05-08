package cc.bitky.clustermanage.tcp.server.channelhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cc.bitky.clustermanage.server.bean.ServerTcpMessageHandler;
import cc.bitky.clustermanage.server.message.base.BaseTcpResponseMsg;
import cc.bitky.clustermanage.server.message.base.IMessage;
import cc.bitky.clustermanage.server.message.tcp.TcpMsgResponseStatus;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Service
@ChannelHandler.Sharable
public class ParsedMessageInBoundHandler extends SimpleChannelInboundHandler<IMessage> {
    private final ServerTcpMessageHandler serverTcpMessageHandler;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ParsedMessageInBoundHandler(ServerTcpMessageHandler serverTcpMessageHandler) {
        this.serverTcpMessageHandler = serverTcpMessageHandler;
    }

    ServerTcpMessageHandler getServerTcpMessageHandler() {
        return serverTcpMessageHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMessage msg) {
        //检测捕获到的帧异常「限定 ID 范围：设备组『1 - 100』；设备『1 - 100』」
        if (msg.getBoxId() != -2 && (msg.getGroupId() <= 0 || msg.getGroupId() > 100 || msg.getBoxId() <= 0 || msg.getBoxId() > 100))
            return;

        //将常规回复帧信息传入「常规回复信息处理方法」
        if (msg.getMsgId() > 0x40 && msg.getMsgId() <= 0x4F) {
            serverTcpMessageHandler.handleTcpResponseMsg((BaseTcpResponseMsg) msg);
            return;
        }

        if (msg.getMsgId() > 0x70 && msg.getMsgId() <= 0x7F) {
            serverTcpMessageHandler.handleTcpResponseMsg((BaseTcpResponseMsg) msg);
            return;
        }

        if (msg.getMsgId() == 0x40) {
            serverTcpMessageHandler.handleResDeviceStatus((TcpMsgResponseStatus) msg);
            return;
        }

        //将初始化帧信息传入「初始化信息处理方法」
        byte a0 = (byte) 0xA0;
        byte af = (byte) 0xAF;
        if (msg.getMsgId() >= a0 && msg.getMsgId() <= af) {
            serverTcpMessageHandler.handleTcpInitMsg(msg);
            return;
        }

        //将其余功能帧信息传入「功能信息处理方法」
        serverTcpMessageHandler.handleTcpMsg(msg);
    }
}
