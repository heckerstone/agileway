package com.jn.agileway.ssh.test.channel.direct.session.command;

import com.jcraft.jsch.JSch;
import com.jn.agileway.ssh.client.AbstractSshConnectionConfig;
import com.jn.agileway.ssh.client.SshConnection;
import com.jn.agileway.ssh.client.SshConnectionFactory;
import com.jn.agileway.ssh.client.SshException;
import com.jn.agileway.ssh.client.impl.ganymedssh2.Ssh2ConnectionConfig;
import com.jn.agileway.ssh.client.impl.ganymedssh2.Ssh2ConnectionFactory;
import com.jn.agileway.ssh.client.impl.jsch.JschConnectionConfig;
import com.jn.agileway.ssh.client.impl.jsch.JschConnectionFactory;
import com.jn.agileway.ssh.client.impl.jsch.JschGlobalProperties;
import com.jn.agileway.ssh.client.supports.command.SshCommandLineExecutor;
import com.jn.langx.commandline.CommandLine;
import com.jn.langx.commandline.DefaultExecuteResultHandler;
import com.jn.langx.commandline.streamhandler.OutputAsStringExecuteStreamHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SshCommandLineExecutorTest {
    private static Logger logger = LoggerFactory.getLogger(SshCommandLineExecutorTest.class);


    @Test
    public void testJsch() throws Throwable {
        JschGlobalProperties jschGlobalProperties = new JschGlobalProperties();
        jschGlobalProperties.apply();

        JSch jsch = new JSch();
        jsch.setKnownHosts("known_hosts");

        testExec(new JschConnectionFactory(), new JschConnectionConfig());
    }


    @Test
    public void testGanymedSsh2() throws Throwable {
        testExec(new Ssh2ConnectionFactory(), new Ssh2ConnectionConfig());
    }


    private void testExec(SshConnectionFactory connectionFactory, AbstractSshConnectionConfig connectionConfig) throws SshException, IOException {
        connectionConfig.setHost("192.168.1.79");
        connectionConfig.setPort(22);
        connectionConfig.setUser("fangjinuo");
        connectionConfig.setPassword("fjn13570");
        SshConnection connection = connectionFactory.get(connectionConfig);

        SshCommandLineExecutor executor = new SshCommandLineExecutor(connection);
        executor.setWorkingDirectory(new File("~/.java"));


        OutputAsStringExecuteStreamHandler output = new OutputAsStringExecuteStreamHandler();
        executor.setStreamHandler(output);

        executor.execute(CommandLine.parse("ifconfig"));
        showResult(executor);

        System.out.println("====================================");

        executor.execute(CommandLine.parse("ls -al"));
        showResult(executor);


        connection.close();
    }

    private static void showResult(SshCommandLineExecutor executor) {
        DefaultExecuteResultHandler resultHandler = (DefaultExecuteResultHandler) executor.getResultHandler();
        if (resultHandler.hasResult()) {
            Throwable exception = resultHandler.getException();
            if (exception != null) {
                logger.error(exception.getMessage(), exception);
            } else {
                OutputAsStringExecuteStreamHandler output = (OutputAsStringExecuteStreamHandler) executor.getStreamHandler();
                String str = output.getOutputContent();
                logger.info(str);
            }
        }
    }
}
