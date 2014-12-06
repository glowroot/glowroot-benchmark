#!/bin/sh -e

run_iterations=$1

printf "\n==============================================================\n"
printf "                          %12s%12s%12s\n" "baseline" "glowroot" "overhead"

printf "response times\n"

# response time: average
for run in "baseline" "glowroot"
do
  for ((i = 1; i <= $run_iterations; i++))
  do
    awk -F $'\t' '$3 ~ /REQUEST/ { sum += $9 - $6; count++ } END { printf("%.1f\n", sum / count) }' results/$run-$i/heatclinicsimulation-*/simulation.log
  done | sort -n | awk '{a[NR-1] = $1} END { printf "%f ", a[int(NR*0.5-0.5)] }'
done | awk '{ printf "  average                 %9.1f ms%9.1f ms%10.1f %%\n", $1, $2, 100*($2-$1)/$1 }'

# response time: 95th percentile
for run in "baseline" "glowroot"
do
  for ((i = 1; i <= $run_iterations; i++))
  do
    cat results/$run-$i/heatclinicsimulation-*/simulation.log | awk -F $'\t' '$3 ~ /REQUEST/ { print $9 - $6 }' | sort -n | awk '{ duration[NR-1] = $1 } END { printf "%d\n", duration[int(NR*0.95-0.5)] }'
  done | sort -n | awk '{a[NR-1] = $1} END { printf "%d ", a[int(NR*0.5-0.5)] }'
done | awk '{ printf "  95th percentile         %9d ms%9d ms%10.1f %%\n", $1, $2, 100*($2-$1)/$1 }'

# response time: 99th percentile
for run in "baseline" "glowroot"
do
  for ((i = 1; i <= $run_iterations; i++))
  do
    cat results/$run-$i/heatclinicsimulation-*/simulation.log | awk -F $'\t' '$3 ~ /REQUEST/ { print $9 - $6 }' | sort -n | awk '{ duration[NR-1] = $1 } END { printf "%d\n", duration[int(NR*0.99-0.5)] }'
  done | sort -n | awk '{a[NR-1] = $1} END { printf "%d ", a[int(NR*0.5-0.5)] }'
done | awk '{ printf "  99th percentile         %9d ms%9d ms%10.1f %%\n", $1, $2, 100*($2-$1)/$1 }'

# requests per second
printf "\n"
for run in "baseline" "glowroot"
do
  for ((i = 1; i <= $run_iterations; i++))
  do
    cat results/$run-$i/requests-per-second
  done | sort -n | awk '{a[NR-1] = $1} END { printf "%f ", a[int(NR*0.5-0.5)] }'
done | awk '{ printf "requests per second       %12.1f%12.1f%10.1f %%\n", $1, $2, 100*($1-$2)/$1 }'

# cpu utilization
printf "\n"
for run in "baseline" "glowroot"
do
  for ((i = 1; i <= $run_iterations; i++))
  do
    cat results/$run-$i/cpu-utilization
  done | sort -n | awk '{a[NR-1] = $1} END { printf "%f ", a[int(NR*0.5-0.5)] }'
done | awk '{ printf "cpu utilization           %10.1f %%%10.1f %%%10.1f %%\n", $1, $2, 100*($2-$1)/$1 }'

# application startup
printf "\n"
for run in "baseline" "glowroot"
do
  for ((i = 1; i <= $run_iterations; i++))
  do
    cat results/$run-$i/application-startup
  done | sort -n | awk '{a[NR-1] = $1} END { printf "%d ", a[int(NR*0.5-0.5)] }'
done | awk '{ printf "application startup       %9d ms%9d ms%10.1f %%\n", $1, $2, 100*($2-$1)/$1 }'

printf "==============================================================\n\n"
