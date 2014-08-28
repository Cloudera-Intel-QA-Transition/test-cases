package org.apache.itest.failures

import org.apache.bigtop.itest.failures.AbstractFailure
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.itest.util.CMClient

class CMRoleDownFailure extends AbstractFailure {
    static private Log LOG = LogFactory.getLog(CMRoleDownFailure.class);

    private static final SLEEP_TIME = 100;
    private String service;
    private String role;
    private CMClient client = new CMClient();
    private boolean waiting = false;
    /**
     * Simple constructor for failures, uses default values.
     * @param hosts list of hosts this failure will be executed on.
     */
    CMRoleDownFailure(List<String> hosts, String service, String role) {
        super(hosts);
        this.service = service;
        this.role = role;
    }

    /**
     * Constructor allowing to set all params.
     *
     * @param hosts list of hosts the failure will be running against
     * @param startDelay how long (in millisecs) failure will wait before starting
     */
    CMRoleDownFailure(List<String> hosts, String service, String role, long startDelay) {
        super(hosts, startDelay)
        this.service = service;
        this.role = role;
    }

    @Override
    public void run() {
        try {
            if (startDelay > 0) {
                try {
                    Thread.sleep(startDelay)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                    return
                }
            }

            runFailCommands()

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    waiting = true;
                    Thread.sleep(SLEEP_TIME)
                } catch (InterruptedException e) {
                    return
                }
            }
        } finally {
            waiting = false;
            runRestoreCommands()
        }
    }

    public boolean isWaiting(){
        return waiting;
    }

    private void runRestoreCommands() {
        hosts.each {
            String theRole = client.findRoleByHost(it, service, role);
            client.startProcess(theRole);
            LOG.info("Starting ${theRole}")
            assert client.waitForProcess(theRole, true, 60);
            LOG.info("${theRole} is started")
        }
    }

    private void runFailCommands() {
        hosts.each {
            String theRole = client.findRoleByHost(it, service, role);
            client.stopProcess(theRole);
            LOG.info("Stopping ${theRole}")
            assert client.waitForProcess(theRole, false, 60);
            LOG.info("${theRole} is stopped")
        }
    }

}
