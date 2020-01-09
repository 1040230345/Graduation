package com.example.demo.service;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import com.example.demo.utils.SSHAgent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@ServerEndpoint(value = "/websocket")
@Component
public class WebSocketServer {
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    //操作SSH的工具类
    private SSHAgent sshAgent;
    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        log.info("客户端连接！");
        this.session = session;
        try {
            //在客户端通过webSocket连接时，创建一个SSH的会话
            this.sshAgent = new SSHAgent();
            //配置
            this.sshAgent.initSession("192.168.72.128", "jackson", "123456");
            //准备执行命令。
            sshAgent.execCommand(this);
            //导入环境
//            this.sshAgent.printWriter.write( "docker pull tomcat "+ "\r\n");
//            this.sshAgent.printWriter.write( "docker run -p 8080:8080 tomcat "+ "\r\n");
            //this.sshAgent.printWriter.flush();
        } catch (IOException e) {
            log.error("websocket IO异常");
        }
    }
    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        this.sshAgent.close();
        log.info("有一连接关闭！");
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message) {
        log.info("来自客户端的消息:" + message);
        //群发消息
        try {
            //通过工具类的标准输入网远程服务器中写内容
            this.sshAgent.printWriter.write( message+ "\r\n");
            this.sshAgent.printWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        error.printStackTrace();
    }

    public void sendMessage(String message) throws IOException {
        //System.out.println(message);
        this.session.getBasicRemote().sendText(message);
    }
}
