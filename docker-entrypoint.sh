#!/bin/bash
set -e

if [[ $@ ]]; then
  exec "$@"
fi

: ${JVM_ARGS:=-Xmx1g -Xms1g}

if [[ -n $GLOWROOT_DIST_ZIP ]]; then
  rm -rf glowroot
  unzip $GLOWROOT_DIST_ZIP
fi

echo '{"advanced":{"captureThreadInfo":false,"captureGcInfo":false}}' > glowroot/config.json

java $HARNESS_JVM_ARGS -jar benchmarks.jar $JMH_ARGS -rf json -prof gc \
  -jvmArgs "$JVM_ARGS -Djava.security.egd=file:/dev/urandom -Djmh.shutdownTimeout=0 -Djmh.shutdownTimeout.step=0"

java -cp benchmarks.jar org.glowroot.benchmark.ResultFormatter jmh-result.json
