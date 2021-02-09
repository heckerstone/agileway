package com.jn.agileway.ssh.client;

import com.jn.agileway.ssh.client.channel.SessionedChannel;
import com.jn.agileway.ssh.client.supports.command.SshCommandResponse;
import com.jn.langx.annotation.NonNull;
import com.jn.langx.annotation.Nullable;
import com.jn.langx.util.Emptys;
import com.jn.langx.util.Preconditions;
import com.jn.langx.util.Strings;
import com.jn.langx.util.collection.Collects;
import com.jn.langx.util.function.Consumer2;
import com.jn.langx.util.io.Charsets;
import com.jn.langx.util.io.IOs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class SshClients {
    private static final Logger logger = LoggerFactory.getLogger(SshClients.class);

    public static SshCommandResponse exec(@NonNull SshConnection connection, @NonNull String command) throws SshException {
        return exec(connection, null, null, command, null);
    }

    /**
     * 通过ssh 连接到远程机器上，cd到指定的workingDirectory下， 执行命令
     *
     * @param connection
     * @param environmentVariables 环境变量
     * @param workingDirectory     工作目录
     * @param command              要执行的命令
     * @param encoding             输出内容的编码，默认为 UTF-8
     * @return
     * @throws SshException
     */
    public static SshCommandResponse exec(@NonNull SshConnection connection, @Nullable Map<String, String> environmentVariables, @Nullable String workingDirectory, @NonNull String command, @Nullable String encoding) throws SshException {
        Preconditions.checkState(connection != null && connection.isConnected() && !connection.isClosed(), "connection status invalid");
        Preconditions.checkNotEmpty(command, "the command is not supplied");
        Charset charset = Charsets.UTF_8;
        if (Strings.isNotEmpty(encoding)) {
            try {
                charset = Charsets.getCharset(encoding);
            } catch (Throwable ex) {
                logger.warn("The encoding is invalid : {}", encoding);
            }
        }

        final SessionedChannel channel = connection.openSession();
        if (Emptys.isNotEmpty(environmentVariables)) {
            Collects.forEach(environmentVariables, new Consumer2<String, String>() {
                @Override
                public void accept(String variable, String value) {
                    channel.env(variable, value);
                }
            });
        }

        if (Strings.isNotEmpty(workingDirectory)) {
            workingDirectory = workingDirectory.replace("\\", "/");
            command = "cd " + workingDirectory + ";" + command;
        }

        channel.exec(command);
        int exitStatus = channel.getExitStatus();

        SshCommandResponse response = new SshCommandResponse();
        response.setExitStatus(exitStatus);
        try {
            if (exitStatus != 0) {
                InputStream errorInputStream = channel.getErrorInputStream();
                byte[] errorContent = IOs.toByteArray(errorInputStream);
                String error = new String(errorContent, charset);
                response.setExitErrorMessage(error);
            } else {
                InputStream inputStream = channel.getInputStream();
                byte[] outputContent = IOs.toByteArray(inputStream);
                String output = new String(outputContent, charset);
                response.setResult(output);
            }
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
        return response;
    }
}
