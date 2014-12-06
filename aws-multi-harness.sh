#!/bin/sh -e

: ${AWS_ACCESS_KEY_ID:?}
: ${AWS_SECRET_ACCESS_KEY:?}
: ${AWS_DEFAULT_REGION:?}

: ${IMAGE_ID:=ami-b5a7ea85}
: ${INSTANCE_TYPE:=t2.micro}
: ${SECURITY_GROUP:=default}
: ${KEY_NAME:=glowroot-benchmark}
: ${PRIVATE_KEY_FILE:=$HOME/.ssh/glowroot-benchmark.pem}
: ${GLOWROOT_DIST_ZIP:=../glowroot/distribution/target/glowroot-dist.zip}

echo creating instance ...
# needs larger than default ebs volume to store all the result data
instance_id=`aws ec2 run-instances --image-id $IMAGE_ID --count 1 --instance-type $INSTANCE_TYPE --key-name $KEY_NAME --security-groups $SECURITY_GROUP --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":20}}]' | grep InstanceId | cut -d '"' -f4`

# suppress stdout (but not stderr)
aws ec2 create-tags --resources $instance_id --tags Key=Name,Value=glowroot-benchmark-driver > /dev/null

echo instance created: $instance_id

while
  public_dns_name=`aws ec2 describe-instances --instance-ids $instance_id --filters Name=instance-state-name,Values=running --query 'Reservations[].Instances[].PublicDnsName' --output text`
  [ ! $public_dns_name ]
do
  echo waiting for instance to start ...
  sleep 1
done

echo instance started: $public_dns_name

while
  # intentionally suppress both stdout and stderr
  ssh -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no ec2-user@$public_dns_name echo &> /dev/null
  [ "$?" != "0" ]
do
  echo waiting for sshd to start ...
  sleep 1
done

script="
chmod 600 .ssh/id_rsa
export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION

export GLOWROOT_DIST_ZIP=glowroot-dist.zip
export PRIVATE_KEY_FILE=~/.ssh/id_rsa

mkdir -p output
for i in {1..9}
do
  LOCAL_RESULTS_DIR=result-data/instance-\$i nohup ./aws-single-harness.sh &> output/instance-\$i.out &
done

mkdir -p output-ehcache-disabled
for i in {1..9}
do
  LOCAL_RESULTS_DIR=result-data-ehcache-disabled/instance-\$i EHCACHE_DISABLED=true nohup ./aws-single-harness.sh &> output-ehcache-disabled/instance-\$i.out &
done

while
  ps -ef | grep aws-single-harness.sh | grep -v grep &> /dev/null
  [ \"\$?\" == \"0\" ]
do
  echo
  echo waiting for benchmarks to complete ...
  echo
  sleep 10
  # monitor output file sizes to ensure all benchmarks are proceeding normally
  ls -l output output-ehcache-disabled
done

echo
echo tarring result data ...
tar czf result-data.tar.gz result-data
tar czf output.tar.gz output
tar czf result-data-ehcache-disabled.tar.gz result-data-ehcache-disabled
tar czf output-ehcache-disabled.tar.gz output-ehcache-disabled
"

scp -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no $PRIVATE_KEY_FILE ec2-user@$public_dns_name:.ssh/id_rsa
scp -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no $GLOWROOT_DIST_ZIP *.sh *.scala heatclinic.* ec2-user@$public_dns_name:.

ssh -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no ec2-user@$public_dns_name "$script"

echo copying result data ...
scp -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no ec2-user@$public_dns_name:\{result-data,output\}.tar.gz .
scp -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no ec2-user@$public_dns_name:\{result-data,output\}-ehcache-disabled.tar.gz .

echo terminating instance ...
aws ec2 terminate-instances --instance-ids $instance_id > /dev/null
