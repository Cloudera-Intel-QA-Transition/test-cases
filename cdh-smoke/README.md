#Introdution

This is a project to develop and build CDH related smoke and system tests. It's based on Bigtop itest framework.
To build:

1. checkout latest [Apache Bigtop](https://github.com/apache/bigtop)
2. in the top-level folder of bigtop: mvn install
3. in bigtop-test-framework: mvn install -DskipTests
4. in bigtop-tests/test-artifacts: mvn install
5. checkout latest [cdh-smoke](https://github.com/Cloudera-Intel-QA-Transition/test-cases/cdh-smoke)
6. in the top-level folder of cdh-smoke: mvn package
7. copy target/cdh-smoke-0.8.0-SNAPSHOT-shaded.jar to a CDH gateway node

#Hive Metastore HA
To run Metastore failover cases:
```bash
export HIVE_CONF_DIR=/etc/hive/conf
java -jar cdh-smoke-0.8.0-SNAPSHOT-shaded.jar org.apache.itest.cdhsmoke.hive.TestMetastoreHA
```
To run hdfs HDFS failover cases:
```bash
java -jar cdh-smoke-0.8.0-SNAPSHOT-shaded.jar org.apache.itest.cdhsmoke.hive.TestHdfsHA
```

