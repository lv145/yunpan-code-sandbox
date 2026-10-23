package com.lgg.yunpancodesandbox.docker;


import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.ExecStartResultCallback;



import java.nio.charset.StandardCharsets;

import java.util.concurrent.atomic.AtomicBoolean;


public class DockerCmdResultCallback extends ExecStartResultCallback {
    private final AtomicBoolean outputLimitExceeded;
    private final StringBuilder resultBuilder;
    private final StringBuilder errorBuilder;
    private final int maxOutputLength;

    public DockerCmdResultCallback(StringBuilder resultBuilder, StringBuilder errorBuilder, int maxOutputLength, AtomicBoolean outputLimitExceeded) {
        this.resultBuilder = resultBuilder;
        this.errorBuilder = errorBuilder;
        this.maxOutputLength = maxOutputLength;
        this.outputLimitExceeded = outputLimitExceeded;

    }
    @Override
    public void onNext(Frame frame) {
        if (outputLimitExceeded.get()) {
            return;
        }
        String message = new String(frame.getPayload(), StandardCharsets.UTF_8);
        if (StreamType.STDERR.equals(frame.getStreamType())) {
            errorBuilder.append(message);
        } else if (resultBuilder.length() + message.length() > maxOutputLength) {
            outputLimitExceeded.set(true);
        } else {
            resultBuilder.append(message);
        }
    }



}
