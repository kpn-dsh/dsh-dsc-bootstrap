mkdir -p /opt/docker/tmp
export TMPDIR=/opt/docker/tmp
export TMP=/opt/docker/tmp
export TEMP=/opt/docker/tmp
#=== ENABLE JMX =======================================================================================

export JMX_PORT=9999
export IP_ADDRESS=$(cat /etc/hosts | grep $HOSTNAME | awk '{ print $1; }')
addJava "-Djava.rmi.server.hostname=$IP_ADDRESS -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.rmi.port=$JMX_PORT -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

#=== ENABLE PROFILER ===========================================================================

export PROFILER_PORT=9998
if [ "x$PROFILER" = "xhonest" ] ; then
    addJava "-agentpath:/opt/docker/liblagent.so=start=0,logPath=/tmp/profile.hpl,host=$IP_ADDRESS,port=$PROFILER_PORT"
elif [ "x$PROFILER" = "xyourkit" ] ; then
    addJava "-agentpath:/opt/docker/libyjpagent.so=listen=$IP_ADDRESS,port=$PROFILER_PORT,logdir=/tmp"
fi

#=== START KAFKA-SECURITY CONFIG ======================================================================
#set -x

export PKI_CONFIG_DIR=/tmp/kafka/config
mkdir -p $PKI_CONFIG_DIR

get_property() {
    local propfile="$1"
    local prop="$2"

    cat "$propfile" | grep "$prop" | head -n 1 | sed 's/^[^=:]*[=:][ 	]*//'
}

if [ ! -z $DSH_SECRET_TOKEN ] ; then
  KAFKA_GROUP=`echo ${MARATHON_APP_ID} | cut -d / -f 2`
  DNS=`echo ${MARATHON_APP_ID} | tr "/" "\n" | sed '1!G;h;$!d' |grep -v "^$" |tr "\n" "." | sed "s/$/marathon.mesos/g"`
  echo "${DSH_CA_CERTIFICATE}" > ${PKI_CONFIG_DIR}/ca.crt
  DN=`curl --cacert ${PKI_CONFIG_DIR}/ca.crt -s "${DSH_KAFKA_CONFIG_ENDPOINT}/dn/${KAFKA_GROUP}/${MESOS_TASK_ID}"`
  if echo "${DN}" | grep "^CN="
  then
          echo "DN OK"
  else
          echo "Could not get distinguished name: " ${DN}
          exit -1
  fi

  PKI_KEYPASS=`(tr -dc A-Za-z0-9 < /dev/urandom | head -c32)`
  PKI_TRUSTPASS="$PKI_KEYPASS"
  PKI_STOREPASS="$PKI_KEYPASS"
  PKI_TRUSTSTORE=${PKI_CONFIG_DIR}/truststore.jks
  PKI_KEYSTORE=${PKI_CONFIG_DIR}/keystore.jks

  rm -f ${PKI_KEYSTORE} ${PKI_TRUSTSTORE}

  keytool -importcert -noprompt -trustcacerts -alias ca -file ${PKI_CONFIG_DIR}/ca.crt -storepass ${PKI_TRUSTPASS} -keystore ${PKI_TRUSTSTORE}
  keytool -importcert -noprompt -trustcacerts -alias ca -file ${PKI_CONFIG_DIR}/ca.crt -storepass ${PKI_STOREPASS} -keypass ${PKI_KEYPASS} -keystore ${PKI_KEYSTORE}
  keytool -genkey -dname "${DN}" -alias server -keyalg RSA -keysize 2048 -storepass ${PKI_STOREPASS} -keypass ${PKI_KEYPASS} -keystore ${PKI_KEYSTORE}
  keytool -certreq -alias server -ext san=dns:${DNS} -file ${PKI_CONFIG_DIR}/server.csr -storepass ${PKI_STOREPASS} -keypass ${PKI_KEYPASS} -keystore ${PKI_KEYSTORE}
  curl --cacert ${PKI_CONFIG_DIR}/ca.crt -s -X POST --data-binary @${PKI_CONFIG_DIR}/server.csr -H "X-Kafka-Config-Token: ${DSH_SECRET_TOKEN}" "${DSH_KAFKA_CONFIG_ENDPOINT}/sign/${KAFKA_GROUP}/${MESOS_TASK_ID}" > ${PKI_CONFIG_DIR}/server.crt
  keytool -importcert -alias server -file ${PKI_CONFIG_DIR}/server.crt -storepass ${PKI_STOREPASS} -keypass ${PKI_KEYPASS} -keystore ${PKI_KEYSTORE}

  # fetch Kafka bootstrap broker list and stream configuration from PKI
  # we need to jump through some hoops to get to the client cert and key in a format that is convenient for curl
  keytool -importkeystore -srckeystore ${PKI_KEYSTORE} -destkeystore ${PKI_CONFIG_DIR}/client.pfx -deststoretype PKCS12 -srcalias server -srcstorepass ${PKI_STOREPASS} -srckeypass ${PKI_KEYPASS} -deststorepass ${PKI_STOREPASS} -destkeypass ${PKI_KEYPASS}
  openssl pkcs12 -in ${PKI_CONFIG_DIR}/client.pfx -out ${PKI_CONFIG_DIR}/client.p12 -nodes -passin pass:${PKI_STOREPASS}
  curl -sf --cacert ${PKI_CONFIG_DIR}/ca.crt --cert ${PKI_CONFIG_DIR}/client.p12:${PKI_KEYPASS} "${DSH_KAFKA_CONFIG_ENDPOINT}/kafka/config/${KAFKA_GROUP}/${MESOS_TASK_ID}?format=java" > ${PKI_CONFIG_DIR}/datastreams.properties

  # Remove intermediate files
  rm -f ${PKI_CONFIG_DIR}/server.csr ${PKI_CONFIG_DIR}/server.crt ${PKI_CONFIG_DIR}/client.pfx ${PKI_CONFIG_DIR}/client.p12

  # Generate kafka config properties file
  cat >> ${PKI_CONFIG_DIR}/datastreams.properties <<EOF
security.protocol=SSL
ssl.truststore.location=${PKI_TRUSTSTORE}
ssl.truststore.password=${PKI_TRUSTPASS}
ssl.keystore.location=${PKI_KEYSTORE}
ssl.keystore.password=${PKI_STOREPASS}
ssl.key.password=${PKI_KEYPASS}
EOF

  export KAFKA_SECURITY_PROTOCOL=SSL
  export KAFKA_SSL_TRUSTSTORE_LOCATION=$PKI_CONFIG_DIR/truststore.jks
  export KAFKA_SSL_TRUSTSTORE_PASSWORD=${PKI_TRUSTPASS}
  export KAFKA_SSL_KEYSTORE_LOCATION=$PKI_CONFIG_DIR/keystore.jks
  export KAFKA_SSL_KEYSTORE_PASSWORD=${PKI_STOREPASS}
  export KAFKA_SSL_KEY_PASSWORD=${PKI_KEYPASS}
  export KAFKA_PRIVATE_CONSUMER_GROUPS="$(get_property "${PKI_CONFIG_DIR}/datastreams.properties" consumerGroups.private)"
  export KAFKA_SHARED_CONSUMER_GROUPS="$(get_property "${PKI_CONFIG_DIR}/datastreams.properties" consumerGroups.shared)"
  export KAFKA_SHARED_CONSUMER_GROUP="$(echo $KAFKA_SHARED_CONSUMER_GROUPS | sed 's/ *,.*//')"
  export KAFKA_BOOTSTRAP_SERVERS="$(get_property "${PKI_CONFIG_DIR}/datastreams.properties" bootstrap.servers)"
fi
#=== END KAFKA-SECURITY CONFIG ======================================================================

echo "STARTING - app: ${app_mainclass} with JAVA options: ${java_opts}"
