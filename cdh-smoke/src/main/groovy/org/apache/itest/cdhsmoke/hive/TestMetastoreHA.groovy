package org.apache.itest.cdhsmoke.hive

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.Variable
import org.apache.bigtop.itest.shell.Shell
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.itest.failures.CMRoleDownFailure
import org.apache.itest.failures.MetastoreLinkFailure
import org.apache.itest.failures.ServiceSuspendFailure
import org.apache.itest.util.CMClient
import org.junit.AfterClass
import org.junit.BeforeClass;
import org.junit.Test
import org.apache.bigtop.itest.Contract;
import org.apache.bigtop.itest.ParameterSetter;
import org.apache.bigtop.itest.Property

import static junit.framework.Assert.assertEquals

@Contract(
    properties = [],
    env = [@Variable(name="HIVE_CONF_DIR")])
class TestMetastoreHA {
    static private Log LOG = LogFactory.getLog(TestMetastoreHA.class);

    public static String HIVE_CONF_DIR;

    static String testRoot;

    static Shell sh = new Shell("/bin/bash -s");

    @BeforeClass
    public static void setUp() {
        ParameterSetter.setEnv(TestMetastoreHA.class);
        File resource = new File(TestMetastoreHA.class.simpleName);

        if (!resource.exists()) {
            try{
                JarContent.unpackJarContainer(TestMetastoreHA.class, resource.absolutePath , "scripts");
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

    /* case1:
     * Hive CLI executes {CREATE, ALTER, DESCRIBE} DDLs for many times
     * The first HMS is selected to be terminated (kill -9)
     */
    @Test
    public void testHMSDown(){
        Configuration config = new Configuration();
        config.addResource(new Path("${HIVE_CONF_DIR}/hive-site.xml"));
        String value = config.get("hive.metastore.uris");
        LOG.info("HMS URIs: $value");
        String[] uris = value.split(",");
        if(uris.length > 1){
            URI u = URI.create(uris[0]);
            String host = u.getHost();
            LOG.info("${host} is selected down");
            def HMSFailure = new CMRoleDownFailure([host], "hive", "METASTORE", 5000);
            Thread t = new Thread(HMSFailure);
            t.start()
            try{
                runScript("simple", null)
            }
            finally{
                t.interrupt()
                t.join();
            };
        }
        else{
            LOG.error("Skipped testHMSDown as there's only 1 HMS")
        }
    }

    /* case2:
     * Hive CLI executes {CREATE, ALTER, DESCRIBE} DDLs for many times
     * The first HMS is selected to be suspended (kill -19)
     * client socket timeout is reset to a smaller one (default is 5 min for CDH) to shorten fail over time
     */
    @Test
    public void testHMSSuspended(){
        Configuration config = new Configuration();
        config.addResource(new Path("${HIVE_CONF_DIR}/hive-site.xml"));
        String value = config.get("hive.metastore.uris");
        LOG.info("HMS URIs: $value");
        String[] uris = value.split(",");
        if(uris.length > 1){
            URI u = URI.create(uris[0]);
            String host = u.getHost();
            LOG.info("${host} is selected to suspend");
            def HMSFailure = new ServiceSuspendFailure([host], "METASTORE", 5000);
            Thread t = new Thread(HMSFailure);
            t.start()
            try{
                runScript("simple", "--hiveconf hive.metastore.client.socket.timeout=10")
            }
            finally{
                t.interrupt()
                t.join();
            };
        }
        else{
            LOG.error("Skipped testHMSDown as there's only 1 HMS")
        }
    }

    private String findRemoteHMS(String[] uris){
        def localName = sh.exec("hostname -f").out[0];
        for(String it : uris){
            if(!localName.contains(URI.create(it).getHost()))
                return it;
        }
        return null;
    }

    /* case3:
     * Hive CLI executes {CREATE, ALTER, DESCRIBE} DDLs for many times
     * A remote HMS is selected, and which is moved to first for hive.metastore.uris
     * During Hive CLI running, the HMS link is cut with below rule:
     * iptables -A OUTPUT -p tcp --dport 9083 -d ${HMS} -j DROP
     * This simulates HMS link down, and does not affect other services
     */
    @Test
    public void testHMSLinkDown(){
        Configuration config = new Configuration();
        config.addResource(new Path("${HIVE_CONF_DIR}/hive-site.xml"));
        String value = config.get("hive.metastore.uris");
        LOG.info("HMS URIs: $value");
        String[] uris = value.split(",");
        if(uris.length > 1){
            String hms = findRemoteHMS(uris);
            String host = URI.create(hms).getHost();
            String newURIs = "${hms}," + value.replace("${hms},","");
            LOG.info("${host} is selected to cut link down");
            LOG.info("Using: ${newURIs}");
            def HMSFailure = new MetastoreLinkFailure("localhost", [host], 5000);
            Thread t = new Thread(HMSFailure);
            t.start()
            try{
                runScript("simple", "--hiveconf hive.metastore.uris=${newURIs} --hiveconf hive.metastore.client.socket.timeout=10")
            }
            finally{
                t.interrupt();
                t.join();
            }
        }
        else{
            LOG.error("Skipped testHMSDown as there's only 1 HMS")
        }
    }

}
