#!/bin/bash

#Sakai 3 Demo
export K2_TAG="0.2"

# Treat unset variables as an error when performing parameter expansion
set -o nounset

# environment
export PATH=/usr/local/bin:$PATH
export BUILD_DIR="/home/hybrid"
export JAVA_HOME=/opt/jdk1.6.0_18
export PATH=$JAVA_HOME/bin:${PATH}
export MAVEN_HOME=/usr/local/apache-maven-2.2.1
export M2_HOME=/usr/local/apache-maven-2.2.1
export PATH=$MAVEN_HOME/bin:${PATH}
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"
export JAVA_OPTS="-server -Xmx512m -XX:MaxPermSize=128m -Djava.awt.headless=true -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
export K2_OPTS="-server -Xmx512m -XX:MaxPermSize=128m -Djava.awt.headless=true -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
BUILD_DATE=`date "+%D %R"`

# ensure logs directory exists
if [ ! -d $BUILD_DIR/logs ]
then
	mkdir $BUILD_DIR/logs
fi

# shutdown all running instances
killall -9 java

# Exit immediately if a simple command exits with a non-zero status
set -o errexit

# clean previous builds
cd $BUILD_DIR
rm -rf sakai3
rm -rf ~/.m2/repository/org/sakaiproject

# build sakai 3
echo "Building sakai3/K2@0.2..."
cd $BUILD_DIR
mkdir sakai3
cd sakai3
git clone -q git://github.com/ieb/open-experiments.git
cd open-experiments/slingtests/osgikernel/
git checkout $K2_TAG
mvn clean install -Dmaven.test.skip=true

# start sakai 3 instance
echo "Starting sakai3 instance..."
cd app/target/
java $K2_OPTS -jar org.sakaiproject.nakamura.app-0.2.jar -p 8008 -f - > $BUILD_DIR/logs/sakai3-run.log.txt 2>&1 &

# final cleanup
cd $BUILD_DIR
rm -rf ~/.m2/repository/org/sakaiproject