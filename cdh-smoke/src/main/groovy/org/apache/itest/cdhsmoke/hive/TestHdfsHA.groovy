package org.apache.itest.cdhsmoke.hive

import com.cloudera.api.model.ApiRole
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.ParameterSetter
import org.apache.bigtop.itest.shell.Shell
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.itest.failures.CMRoleDownFailure
import org.apache.itest.util.CMClient
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static junit.framework.Assert.assertEquals

class TestHdfsHA {
    static private Log LOG = LogFactory.getLog(TestHdfsHA.class);

    static String testRoot;

    static Shell sh = new Shell("/bin/bash -s");

    @BeforeClass
    public static void setUp() {
        File resource = new File(TestHdfsHA.class.simpleName);

        if (!resource.exists()) {
            try{
                JarContent.unpackJarContainer(TestHdfsHA.class, resource.absolutePath , "scripts");
            }
            catch(Throwable t){
                LOG.error(t.getMessage());
            }
        }

        testRoot = resource.absolutePath;
    }

    @AfterClass
    public static void tearDown() throws ClassNotFoundException, InterruptedException, NoSuchFieldException, IllegalAccessException {
    }

    public void runScript(String test, String extraArgs){
        String l = "scripts/${test}";
        String test_command="""diff -u <(\$F < ${l}/actual) <(\$F < ${l}/out)""" ;
        sh.exec("""
    cd ${testRoot}
    F=cat
    if [ -f ${l}/filter ]; then
      chmod 777 ${l}/filter
      F=${l}/filter
    fi
    hive ${extraArgs} -v -f ${l}/in > ${l}/actual 2> ${l}/stderr && ${test_command}"""
        ) ;
        assertEquals("Got unexpected output from test script ${test}", 0, sh.ret)
    }

    /* case5:
     * Hive CLI executes some DDLs and HQLs
     * HDFS Active NameNode is terminated during CLI is running
     * The new Active NameNode will also be terminated after some time to switch to original state
     */
    @Test
    public void testFailOver() {
        def client = new CMClient();
        def nameNodes = new String[2];
        def nnRoles = client.findRoles("hdfs", "NAMENODE");
        if(nnRoles.size() > 1){
            if(nnRoles[0].getHaStatus().equals(ApiRole.HaStatus.ACTIVE)){
                nameNodes[0] = client.findHostById(nnRoles[0].getHostRef().getHostId()).getHostname();
                nameNodes[1] = client.findHostById(nnRoles[1].getHostRef().getHostId()).getHostname();
            }
            else{
                nameNodes[0] = client.findHostById(nnRoles[1].getHostRef().getHostId()).getHostname();
                nameNodes[1] = client.findHostById(nnRoles[0].getHostRef().getHostId()).getHostname();
            }
            def failover = new CMRoleDownFailure([nameNodes[0]], "hdfs", "NAMENODE", 20000);
            Thread t1 = new Thread(failover);
            def failback = new CMRoleDownFailure([nameNodes[1]], "hdfs", "NAMENODE", 50000);
            Thread t2 = new Thread(failback);
            t1.start()
            new Thread(new Runnable(){
                @Override
                void run() {
                    while(!failover.isWaiting()){
                        Thread.sleep(5000);
                    }
                    t2.start();
                    t1.interrupt();
                    t1.join();
                }
            }).start();
            try{
                runScript("apachelog", null)
            }
            finally{
                t2.interrupt();
                t2.join();
            }
        }
        else{
            LOG.error("Skipped as HDFS HA is not enabled")
        }
    }
}
