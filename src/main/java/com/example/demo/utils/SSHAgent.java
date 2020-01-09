package com.example.demo.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.example.demo.service.WebSocketServer;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by wolfcode-lanxw
 */
public final class SSHAgent {
    private Connection connection;
    private Session session;
    private BufferedReader stdout;
    public PrintWriter printWriter;
    private BufferedReader stderr;
    private ExecutorService service = Executors.newFixedThreadPool(10);

    public void initSession(String hostName, String userName, String passwd) throws IOException {
        //根据主机名先获取一个远程连接
        connection = new Connection(hostName);
        //发起连接
        connection.connect();
        //认证账号密码
        boolean authenticateWithPassword = connection.authenticateWithPassword(userName, passwd);
        //如果账号密码有误抛出异常
        if (!authenticateWithPassword) {
            throw new RuntimeException("Authentication failed. Please check hostName, userName and passwd");
        }
        //开启一个会话
        session = connection.openSession();
        session.requestDumbPTY();
        //session.requestPTY("bash");
        session.startShell();
        //获取标准输出
        stdout = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStdout()), StandardCharsets.UTF_8));
        //获取标准错误输出
        stderr = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStderr()), StandardCharsets.UTF_8));
        //获取标准输入
        printWriter = new PrintWriter(session.getStdin());
    }

    public void execCommand(final WebSocketServer webSocketServer) throws IOException {
        //执行命令方法，使用线程池来执行
        service.submit(
                new Runnable() {
            @Override
            public void run() {
                String line;
                try {
                    //持续获取服务器标准输出
                    while ((line = stdout.readLine()) != null) {
                        System.out.println(line);
                        //通过对应的webSocket服务端将内容输出到前台
                        webSocketServer.sendMessage(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //关闭资源方法
    public void close() {
        IOUtils.closeQuietly(stdout);
        IOUtils.closeQuietly(stderr);
        IOUtils.closeQuietly(printWriter);
        session.close();
        connection.close();
    }
}