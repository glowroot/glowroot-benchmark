#!/bin/sh -e

: ${TOMCAT_HOME:=/usr/share/tomcat8}
: ${TOMCAT_SERVICE_NAME:=tomcat8}
: ${RUN_DURATION:=1200}
: ${RUN_ITERATIONS:=9}
: ${RUN_USERS:=`nproc`}
: ${JVM_ARGS:="-Xms1g -Xmx1g -XX:+UseG1GC"}
: ${EHCACHE_DISABLED:=false}
: ${GLOWROOT_CONFIG:="{}"}

JVM_ARGS="$JVM_ARGS -javaagent:$TOMCAT_HOME/heatclinic/spring-instrument.jar -Druntime.environment=production -Ddatabase.url=jdbc:mysql://localhost:3306/heatclinic -Ddatabase.user=heatclinic -Ddatabase.password=heatclinic -Ddatabase.driver=com.mysql.jdbc.Driver -Dproperty-shared-override=$TOMCAT_HOME/heatclinic/heatclinic.properties -Dnet.sf.ehcache.disabled=$EHCACHE_DISABLED"

function benchmark {
  run_name=$1
  jvm_args=$2
  run_duration=${3:-$RUN_DURATION}
  mkdir -p results/$run_name

  # make sure tomcat is stopped
  sudo service $TOMCAT_SERVICE_NAME stop

  # set tomcat jvm args
  echo CATALINA_OPTS=\"$jvm_args\" | sudo tee /etc/sysconfig/$TOMCAT_SERVICE_NAME > /dev/null

  # clean up
  sudo sh -c "rm -f $TOMCAT_HOME/logs/*"
  sudo sh -c "rm -f $TOMCAT_HOME/glowroot/*.db"
  sudo sh -c "rm -f $TOMCAT_HOME/glowroot/*.log"
  sudo sh -c "rm -f $TOMCAT_HOME/glowroot/config.json"

  echo $GLOWROOT_CONFIG | sudo tee $TOMCAT_HOME/glowroot/config.json > /dev/null
  sudo chown tomcat:tomcat $TOMCAT_HOME/glowroot/config.json

  # start tomcat
  sudo service $TOMCAT_SERVICE_NAME start

  # wait for tomcat to start
  while
    sleep 5
    sudo sh -c "grep 'Server startup' $TOMCAT_HOME/logs/catalina.*.log"
    [ "$?" != "0" ]
  do
    echo waiting for tomcat to start ...
  done

  # capture application startup time
  sudo sh -c "grep 'Server startup' $TOMCAT_HOME/logs/catalina.*.log" | sed 's/.* \([0-9]* ms\)/\1/' > results/$run_name/application-startup

  # capture memory
  tomcat_pid=`sudo -u tomcat jps | grep Bootstrap | awk '{print $1}'`
  for j in {1..5}
  do
    sudo -u tomcat jcmd $tomcat_pid GC.run
  done
  sudo -u tomcat jstat -gc $tomcat_pid | grep -v OU | awk '{print $8}' > results/$run_name/memory-footprint

  start_cpu_time=`head -1 /proc/stat | awk '{print $2 + $3 + $4}'`
  start_time=`date +%s`
  echo "running $RUN_USERS user(s) $run_duration seconds ..."
  JAVA_OPTS="-Dusers=$RUN_USERS -Dduration=$run_duration" gatling/bin/gatling.sh --simulations-folder $PWD --results-folder $PWD/results/$run_name -s HeatClinicSimulation
  end_cpu_time=`head -1 /proc/stat | awk '{print $2 + $3 + $4}'`
  end_time=`date +%s`
  nproc=`nproc`
  total_requests=`grep REQUEST $PWD/results/$run_name/heatclinicsimulation-*/simulation.log | wc -l`
  echo "scale=1; (($end_cpu_time - $start_cpu_time) / ($end_time - $start_time)) / $nproc" | bc > results/$run_name/cpu-utilization
  echo "scale=1; $total_requests / ($end_time - $start_time)" | bc > results/$run_name/requests-per-second

  # stop tomcat
  sudo service $TOMCAT_SERVICE_NAME stop

  # capture data and logs
  sudo cp -r $TOMCAT_HOME/glowroot results/$run_name/glowroot-data
  sudo chown -R $USER results/$run_name/glowroot-data
  # need cp -L since $TOMCAT_HOME/logs is typically a symbolic link
  sudo cp -rL $TOMCAT_HOME/logs results/$run_name/tomcat-logs
  sudo chown -R $USER results/$run_name/tomcat-logs
}

# run system warmup
benchmark system-warmup "$JVM_ARGS" 30
rm -rf results/system-warmup

for i in $(seq 1 $RUN_ITERATIONS)
do
  # run baseline benchmark
  benchmark baseline-$i "$JVM_ARGS"

  # re-run benchmark with glowroot
  benchmark glowroot-$i "-javaagent:$TOMCAT_HOME/glowroot/glowroot.jar $JVM_ARGS"
done
echo
echo

# display results
./benchmark-report.sh $RUN_ITERATIONS
