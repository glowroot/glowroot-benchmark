#!/bin/sh -e

: ${TOMCAT_HOME:=/usr/share/tomcat8}

# download heatclinic
git clone https://github.com/BroadleafCommerce/DemoSite.git heatclinic
(cd heatclinic && git checkout broadleaf-3.1.10-GA && mvn package)

# download gatling
curl -o gatling.zip http://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts/2.0.3/gatling-charts-highcharts-2.0.3-bundle.zip
unzip gatling.zip
mv gatling-charts-highcharts-* gatling
rm gatling.zip

# download mysql jdbc driver
curl -o mysql-connector-java.jar http://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.33/mysql-connector-java-5.1.33.jar

# create mysql user for heatclinic
mysql --user=root --password=password <<EOF
create user heatclinic@localhost identified by 'heatclinic';
create database heatclinic;
grant all privileges on heatclinic.* to heatclinic@localhost;
EOF

# import database
mysql --user=root --password=password heatclinic < heatclinic.sql

# install mysql jdbc driver
sudo cp mysql-connector-java.jar $TOMCAT_HOME/lib

# install glowroot somewhere tomcat can access
sudo unzip glowroot-dist.zip -d $TOMCAT_HOME
sudo chown -R tomcat:tomcat $TOMCAT_HOME/glowroot

# install spring-instrument javaagent somewhere tomcat can access
sudo mkdir -p $TOMCAT_HOME/heatclinic
sudo cp heatclinic/lib/spring-instrument-*.RELEASE.jar $TOMCAT_HOME/heatclinic/spring-instrument.jar

# install heatclinic war
sudo cp heatclinic/site/target/mycompany.war $TOMCAT_HOME/webapps/ROOT.war

# copy extra heatclinic properties file somewhere tomcat can access
sudo cp heatclinic.properties $TOMCAT_HOME/heatclinic/heatclinic.properties
