#!/bin/bash

# Print commands and their arguments as they are executed.
# set -x
# Exit immediately if a command exits with a non-zero status.
set -e

# Utils
# Just die
die() {
  echo "$@" 1>&2
  exit 1
}
# Run service $1 and check if it has started
runService() {
  systemctl restart $1
  echo `systemctl status $1` | grep -q 'Active: active' || die Service $1 should be running, but it seems dead
}
# When killall does not help
fuzzyKill() {
  kill -9 `ps aux | grep $1 | awk '{print $2}'` 2>/dev/null || true
}

apacheFastestMirror()  {
  curl -s http://www.apache.org/dyn/closer.cgi/?asjson=1 | tail -n 2 | awk '/preferred/ { print $2 }' | tr -d '"'
}

BUILD=/home/vagrant/build
PROVISIONED=/home/vagrant/.provisioned
PSQL="psql -h localhost -U phoenix -d phoenix_development"

mkdir ${BUILD} &> /dev/null || true

# Kill everything before provisioning
echo Stopping services:
STOP_US=( consumer bottledwater kafka elasticsearch zookeeper postgresql )
for STOP_ME in "${STOP_US[@]}"
do
	echo \    ${STOP_ME}
  systemctl stop ${STOP_ME} &> /dev/null || true
done
echo Cleaning the zoo
rm -rf /tmp/* || true
rm -rf /var/lib/zookeeper/* || true

# Combine all external repos to run slow `update` only once
if [ ! -e ${PROVISIONED} ]; then
  echo Adding repositories
  # Java 8
  add-apt-repository ppa:webupd8team/java -y
  echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections
  # Schema registry
  add-apt-repository "deb [arch=all] http://packages.confluent.io/deb/1.0 stable main"
  wget --progress=bar:force -O - http://packages.confluent.io/deb/1.0/archive.key | sudo apt-key add -
  # SBT
  if [[ ! -f /etc/apt/sources.list.d/sbt.list ]]; then
    echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
  fi
  echo Upgrading system and installing packages
  apt-get -y update
  apt-get -y install software-properties-common sbt tmux unzip make curl jq    \
    python-software-properties debconf-utils confluent-schema-registry cmake    \
    oracle-java8-installer oracle-java8-set-default build-essential libpq-dev   \
    libcurl4-openssl-dev libjansson-dev pkg-config librdkafka-dev libsnappy-dev \
    postgresql-server-dev-9.4 postgresql-9.4 postgresql-client-9.4 postgresql-contrib-9.4
  apt-get -y upgrade
fi

touch ${PROVISIONED}

# Build and install dependencies
cd ${BUILD}

APACHE_MIRROR="$(apacheFastestMirror)"

echo Building and hacking dependencies:
echo \    scala
if [ ! -e scala-2.11.7.deb ]; then
  wget http://www.scala-lang.org/files/archive/scala-2.11.7.deb --progress=bar:force
  dpkg -i scala-2.11.7.deb
fi

echo \    kafka
KAFKA_DIR=${BUILD}/kafka
if [[ ! -d ${KAFKA_DIR} ]]; then
  wget -c "${APACHE_MIRROR}/kafka/0.9.0.0/kafka_2.11-0.9.0.0.tgz" --progress=bar:force
  tar -zxf kafka_2.11-0.9.0.0.tgz
  mv kafka_2.11-0.9.0.0 ${KAFKA_DIR}
fi

echo \    elasticsearch
ES_DIR=${BUILD}/elasticsearch
if [[ ! -d ${ES_DIR} ]]; then
  wget -c https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.7.3.zip --progress=bar:force
  unzip -q elasticsearch-1.7.3.zip &&
  mv elasticsearch-1.7.3 ${ES_DIR}
fi

echo \    avro
AVRO=avro-c-1.7.7
if [[ ! -d ${BUILD}/${AVRO} ]]; then
  cd ${BUILD}
  curl -s -o ${AVRO}.tar.gz -SL "${APACHE_MIRROR}/avro/avro-1.7.7/c/avro-c-1.7.7.tar.gz"
  tar -xzf ${AVRO}.tar.gz
  mkdir ${AVRO}/build && cd ${AVRO}/build
  cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local -DCMAKE_BUILD_TYPE=RelWithDebInfo
  make && make test && make install
  ldconfig
fi

echo \    libsnappy
cp /vagrant/vagrant/templates/libsnappy.pc /usr/local/lib/pkgconfig/libsnappy.pc

echo \    bottledwater
BW_DIR=${BUILD}/bottledwater
if [[ ! -d ${BW_DIR} ]]; then
  git clone /bottledwater ${BW_DIR}
  cd ${BW_DIR}
  make clean && make && make install
  cp kafka/bottledwater client/bwtest /usr/local/bin
fi

echo \    flyway
FLYWAY_DIR=${BUILD}/flyway-3.2.1
FLYWAY_HOME=/usr/local/share/flyway-3.2.1
if [[ ! -d ${FLYWAY_HOME} ]]; then
  cd ${BUILD}
  URL=https://bintray.com/artifact/download/business/maven/flyway-commandline-3.2.1-linux-x64.tar.gz
  wget ${URL} -O flyway.tar.gz --progress=bar:force
  tar xzf flyway.tar.gz
  mv ${FLYWAY_DIR} /usr/local/share/
  chmod +x ${FLYWAY_HOME}/flyway
  if ! grep --quiet flyway /etc/profile; then
    echo "FLYWAY_HOME=${FLYWAY_HOME}" >> /etc/profile
    echo "PATH=\$PATH:\$FLYWAY_HOME" >> /etc/profile
  fi
fi

echo \    postgres
PG_CONF=/etc/postgresql/9.4/main/postgresql.conf
PG_LOG=/home/vagrant/pg-logs
mkdir ${PG_LOG} &> /dev/null || true
chown postgres:postgres ${PG_LOG}
# Using 'Phoenix' as a marker that config has already been modified
if ! grep --quiet Phoenix ${PG_CONF}; then
cat >> ${PG_CONF} << EOF

# Phoenix custom settings
listen_addresses = '*'
# Logs
log_statement = all
logging_collector = on
log_directory = '${PG_LOG}'
log_rotation_age = 1d
# Bottledwater
wal_level = logical
max_wal_senders = 8
wal_keep_segments = 4
max_replication_slots = 4
EOF
fi

# Override acces config to allow passwordless login
cp /vagrant/vagrant/templates/pg_hba.conf /etc/postgresql/9.4/main/pg_hba.conf
chmod 644 /etc/postgresql/9.4/main/pg_hba.conf

echo \    schema registry
cp /vagrant/vagrant/templates/schema-registry.properties /home/vagrant/schema-registry.properties

echo \    systemd services
cd /vagrant/vagrant/services
cp * /etc/systemd/system/
for s in *; do systemctl enable $s ; done
systemctl enable postgresql &> /dev/null

# Fun part where everything will try to fail

runService postgresql

echo Configuring DB
sudo -u postgres createuser -s root || {
    echo "postgres: root user already created, ignoring"
}
sudo -u postgres createuser -s vagrant || {
    echo "postgres: vagrant user already created, ignorng"
}

su - postgres -c 'cd /phoenix && make configure'

echo Staring services:

START_US=( zookeeper kafka elasticsearch schema-registry )
for START_ME in "${START_US[@]}"
do
	echo \    ${START_ME}
  runService ${START_ME}
done

# echo Creating bottledwater extension
# ${PSQL} -c 'create extension bottledwater;' || die Failed to create bottledwater!

echo Hang on, need to wait to start bottledwater
sleep 10
echo

echo Starting more services:
echo \    bottledwater
runService bottledwater
echo \    consumer
runService consumer

echo
echo "Allowing unprivileged user to view/filter journalctl logs"
usermod -a -G systemd-journal vagrant

echo
echo All started and probably running! Run:
echo "vagrant ssh -c 'sudo journalctl --pager-end --catalog --follow --lines=100'"
echo "to watch current execution log (probably just SBT being slow)."
echo "Use 'journalctl -u foo -u bar' to only view messages for 'foo' and 'bar' services"
# http://wiki.bash-hackers.org/syntax/pe#substring_removal
for FULL_NAME in /vagrant/vagrant/services/*; do
  SERVICE_NAME=${FULL_NAME##*/} # Remove path (everything up to and including last slash)
  SERVICES="$SERVICES ${SERVICE_NAME%.*} " # Remove extension and append space
done
echo "Service list: $SERVICES"
echo "To test, run:"
echo "curl -s -XGET 'http://localhost:9200/phoenix/customers_search_view/_search' | jq '.'"
echo
echo "VM IP address is 192.168.10.111"
