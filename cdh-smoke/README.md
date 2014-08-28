#Introdution

This is a project to develop and build CDH related smoke and system tests. It's based on Bigtop itest framework.
To build:

1. checkout latest [Apache Bigtop](https://github.com/apache/bigtop)
2. in the top-level folder of bigtop: mvn install
3. in bigtop-test-framework: mvn install -DskipTests
4. in bigtop-tests/test-artifacts: mvn install
5. checkout latest [cdh-smoke](https://github.com/Cloudera-Intel-QA-Transition/test-cases/tree/master/cdh-smoke)
6. in the top-level folder of cdh-smoke: mvn package
7. copy target/cdh-smoke-0.8.0-SNAPSHOT-shaded.jar to a CDH gateway node

#Hive Metastore HA
To run Metastore failover cases:
```bash
export HIVE_CONF_DIR=/etc/hive/conf
java -jar cdh-smoke-0.8.0-SNAPSHOT-shaded.jar org.apache.itest.cdhsmoke.hive.TestMetastoreHA
JUnit version 4.11
.2014-08-28 23:32:49,584 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: HMS URIs: thrift://server-169.novalocal:9083,thrift://server-170.novalocal:9083,thrift://server-171.novalocal:9083
2014-08-28 23:32:49,603 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: server-169.novalocal is selected down
2014-08-28 23:32:49,626 [org.apache.itest.util.CMClient] INFO: CM server: localhost
2014-08-28 23:32:58,135 [org.apache.itest.failures.CMRoleDownFailure] INFO: Stopping hive-HIVEMETASTORE-5c887f3711fed379223f6e65a6f4fbcf
2014-08-28 23:32:58,141 [org.apache.itest.util.CMClient] INFO: Waiting for role hive-HIVEMETASTORE-5c887f3711fed379223f6e65a6f4fbcf stopped
2014-08-28 23:33:08,443 [org.apache.itest.failures.CMRoleDownFailure] INFO: hive-HIVEMETASTORE-5c887f3711fed379223f6e65a6f4fbcf is stopped
2014-08-28 23:33:46,481 [org.apache.itest.failures.CMRoleDownFailure] INFO: Starting hive-HIVEMETASTORE-5c887f3711fed379223f6e65a6f4fbcf
2014-08-28 23:33:46,481 [org.apache.itest.util.CMClient] INFO: Waiting for role hive-HIVEMETASTORE-5c887f3711fed379223f6e65a6f4fbcf started
2014-08-28 23:34:12,033 [org.apache.itest.failures.CMRoleDownFailure] INFO: hive-HIVEMETASTORE-5c887f3711fed379223f6e65a6f4fbcf is started
.2014-08-28 23:34:12,070 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: HMS URIs: thrift://server-169.novalocal:9083,thrift://server-170.novalocal:9083,thrift://server-171.novalocal:9083
2014-08-28 23:34:12,096 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: server-170.novalocal is selected to cut link down
2014-08-28 23:34:12,096 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: Using: thrift://server-170.novalocal:9083,thrift://server-169.novalocal:9083,thrift://server-171.novalocal:9083
.2014-08-28 23:35:13,466 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: HMS URIs: thrift://server-169.novalocal:9083,thrift://server-170.novalocal:9083,thrift://server-171.novalocal:9083
2014-08-28 23:35:13,467 [org.apache.itest.cdhsmoke.hive.TestMetastoreHA] INFO: server-169.novalocal is selected to suspend

Time: 216.98

OK (3 tests)
```
To run hdfs HDFS failover cases:
```bash
#java -jar cdh-smoke-0.8.0-SNAPSHOT-shaded.jar org.apache.itest.cdhsmoke.hive.TestHdfsHA
JUnit version 4.11
.2014-08-28 23:36:54,662 [org.apache.itest.util.CMClient] INFO: CM server: localhost
2014-08-28 23:37:19,030 [org.apache.itest.failures.CMRoleDownFailure] INFO: Stopping hdfs-NAMENODE-5c887f3711fed379223f6e65a6f4fbcf
2014-08-28 23:37:19,039 [org.apache.itest.util.CMClient] INFO: Waiting for role hdfs-NAMENODE-5c887f3711fed379223f6e65a6f4fbcf stopped
2014-08-28 23:37:30,348 [org.apache.itest.failures.CMRoleDownFailure] INFO: hdfs-NAMENODE-5c887f3711fed379223f6e65a6f4fbcf is stopped
2014-08-28 23:37:34,089 [org.apache.itest.failures.CMRoleDownFailure] INFO: Starting hdfs-NAMENODE-5c887f3711fed379223f6e65a6f4fbcf
2014-08-28 23:37:34,090 [org.apache.itest.util.CMClient] INFO: Waiting for role hdfs-NAMENODE-5c887f3711fed379223f6e65a6f4fbcf started
2014-08-28 23:38:07,227 [org.apache.itest.failures.CMRoleDownFailure] INFO: hdfs-NAMENODE-5c887f3711fed379223f6e65a6f4fbcf is started
2014-08-28 23:38:23,943 [org.apache.itest.failures.CMRoleDownFailure] INFO: Stopping hdfs-NAMENODE-922caf562e15338b1625c1af96f06e0d
2014-08-28 23:38:23,943 [org.apache.itest.util.CMClient] INFO: Waiting for role hdfs-NAMENODE-922caf562e15338b1625c1af96f06e0d stopped
2014-08-28 23:38:29,874 [org.apache.itest.failures.CMRoleDownFailure] INFO: hdfs-NAMENODE-922caf562e15338b1625c1af96f06e0d is stopped
2014-08-28 23:38:37,611 [org.apache.itest.failures.CMRoleDownFailure] INFO: Starting hdfs-NAMENODE-922caf562e15338b1625c1af96f06e0d
2014-08-28 23:38:37,611 [org.apache.itest.util.CMClient] INFO: Waiting for role hdfs-NAMENODE-922caf562e15338b1625c1af96f06e0d started
2014-08-28 23:38:59,733 [org.apache.itest.failures.CMRoleDownFailure] INFO: hdfs-NAMENODE-922caf562e15338b1625c1af96f06e0d is started

Time: 125.134

OK (1 test)
```

