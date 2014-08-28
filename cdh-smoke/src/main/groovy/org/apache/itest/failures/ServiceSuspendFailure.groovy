package org.apache.itest.failures

import org.apache.bigtop.itest.Contract
import org.apache.bigtop.itest.ParameterSetter
import org.apache.bigtop.itest.Property
import org.apache.bigtop.itest.failures.AbstractFailure

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Can kill (with kill -9) specified service on specified hosts during tests run.
 */
@Contract(
        properties =
        [@Property(name="ssh.privatekey", type=Property.Type.STRING, defaultValue="~/.ssh/id_dsa"),
         @Property(name="ssh.user", type=Property.Type.STRING, defaultValue="`whoami`"),
        ],
        env = [])
public class ServiceSuspendFailure extends AbstractFailure {
    private static String ssh_privatekey;
    private static String ssh_user;

    private static final String KILL_SERVICE_TEMPLATE = "sudo bash -c \"for a in \\`ls /proc\\`; do if [[ -d /proc/\\\$a/cwd && \\`ls -l /proc/\\\$a/cwd|grep %s\\` ]];then kill -19 \\\$a; fi; done\""
    private static final String START_SERVICE_TEMPLATE = "sudo bash -c \"for a in \\`ls /proc\\`; do if [[ -d /proc/\\\$a/cwd && \\`ls -l /proc/\\\$a/cwd|grep %s\\` ]];then kill -18 \\\$a; fi; done\""
    static{
        ParameterSetter.setProperties(ServiceSuspendFailure.class);
    }
    /**
     * Can kill specified service on specified hosts during tests run.
     *
     * @param hosts list of hosts on which specified service will be killed
     * @param serviceName name of service to be killed.
     */
    public ServiceSuspendFailure(List<String> hosts, String serviceName) {
        super(hosts)
        populateCommandsList(hosts, serviceName)
    }

    /**
     * Can kill specified service on specified hosts during tests run.
     *
     * @param hosts list of hosts on which specified service will be killed
     * @param serviceName name of service to be killed
     * @param startDelay time in milliseconds) the failures will wait before start
     */
    public ServiceSuspendFailure(List<String> hosts,
                                String serviceName,
                                long startDelay) {

        super(hosts, startDelay)
        populateCommandsList(hosts, serviceName)
    }

    protected String getSshWrappedCommand(String formattedCommand, String host) {
        return String.format(SSH_COMMAND_WRAPPER, ssh_privatekey, ssh_user, host, formattedCommand);
    }
    /*
     * Populate commands list, making choice between local execution and remote one.
     */
    private void populateCommandsList(List<String> hosts, String serviceName){
        if (hosts.size() == 1 && "localhost".equalsIgnoreCase(hosts[0])) {
            failCommands.add(String.format(KILL_SERVICE_TEMPLATE, serviceName))
            restoreCommands.add(String.format(START_SERVICE_TEMPLATE, serviceName))
        } else {
            hosts.each { host ->
                failCommands.add(getSshWrappedCommand(String.format(KILL_SERVICE_TEMPLATE, serviceName), host))
                restoreCommands.add(getSshWrappedCommand(String.format(START_SERVICE_TEMPLATE, serviceName), host))
            }
        }
    }
}
