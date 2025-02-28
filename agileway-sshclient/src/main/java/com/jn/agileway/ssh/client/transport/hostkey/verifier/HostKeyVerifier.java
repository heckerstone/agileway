package com.jn.agileway.ssh.client.transport.hostkey.verifier;


/**
 * A callback interface used to implement a client specific method of checking
 * server host keys.
 */

public interface HostKeyVerifier<PUBKEY> {
    /**
     * The actual verifier method, it will be called by the key exchange code
     * on EVERY key exchange - this can happen several times during the lifetime
     * of a connection.
     * <p>
     * Note: SSH-2 servers are allowed to change their hostkey at ANY time.
     *
     * @param hostname the hostname used to create the {@link com.jn.agileway.ssh.client.impl.sshj.SshjConnection} object
     * @param port     the remote TCP port
     * @return if the client wants to accept the server's host key - if not, the
     * connection will be closed.
     * @throws Exception Will be wrapped with an IOException, extended version of returning false =)
     */
    boolean verify(String hostname, int port, String serverHostKeyAlgorithm, PUBKEY publicKey);
}
