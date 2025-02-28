package com.jn.agileway.ssh.client.impl.sshj.verifier;

import com.jn.agileway.ssh.client.transport.hostkey.verifier.HostKeyVerifier;

import java.security.PublicKey;

public class FromSshHostKeyVerifierAdapter implements HostKeyVerifier<PublicKey> {
    private net.schmizz.sshj.transport.verification.HostKeyVerifier delegate;

    public FromSshHostKeyVerifierAdapter(net.schmizz.sshj.transport.verification.HostKeyVerifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean verify(String hostname, int port, String serverHostKeyAlgorithm, PublicKey key) {
        return this.delegate.verify(hostname, port, key);
    }

}
