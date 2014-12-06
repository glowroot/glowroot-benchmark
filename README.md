Glowroot benchmark
=========

Monitoring overhead depends on many factors, but that doesn't mean we shouldn't build solid benchmarks and publish concrete results.

## Methodology

Part of the difficulty in benchmarking the overhead of a monitoring tool is finding a good representative application on which to perform the benchmarking.

The open source [Heat Clinic](http://demo.broadleafcommerce.org) demo application is decently complex (150+ tables) and uses many standard Java libraries (Spring, Hibernate, Quartz, Solr, EHCache), making it an ideal candidate.

Another difficulty is generating reliable enough results to isolate the overhead that the monitoring tool introduces.

To minimize variance, the benchmark is replicated on 9 different EC2 instances and 9 iterations of the benchmark are run on each EC2 instance, with the median result published.

For complete details on benchmark methodology, see [aws-multi-harness.sh](aws-multi-harness.sh).

## Results, part I

Environment:

* Amazon AWS [c3.xlarge](http://aws.amazon.com/ec2/instance-types/#Compute_Optimized)
* Amazon Linux 2014.09.1
* Tomcat 8.0.12
* Oracle JDK 8u25


|                                                 |  without Glowroot  |  with Glowroot  |
| ------------------------------------------------|-------------------:|----------------:|
| response times                                  |                    |                 |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; average          |          33.7 ms   |        34.3 ms  |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 95th percentile  |            77 ms   |          78 ms  |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 99th percentile  |            97 ms   |          99 ms  |
| requests per second                             |             92.0   |           90.6  |
| cpu utilization                                 |           75.1 %   |         75.4 %  |
| application startup                             |         30075 ms   |       32303 ms  |

*(for complete result data from all 9x9 runs, download [result-data.tar.gz](https://glowroot.s3.amazonaws.com/glowroot-benchmark/result-data.tar.gz))*

## Results, part II

Part I is not the most interesting result though, as the demo application is only performing 2 queries per page, so query tracing overhead is expected to be fairly light.

To make things more interesting below, ehcache is disabled, making the demo application perform 217 queries per page.

Even at 217 queries per page, the demo application is pretty fast (117.2 milliseconds under heavy load), ensuring that the monitoring overhead is not buried by a poorly performing application.

|                                                 |  without Glowroot  |  with Glowroot  |
| ------------------------------------------------|-------------------:|----------------:|
| response times                                  |                    |                 |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; average          |         117.2 ms   |       119.2 ms  |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 95th percentile  |           219 ms   |         222 ms  |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 99th percentile  |           259 ms   |         265 ms  |
| requests per second                             |             31.3   |           30.9  |
| cpu utilization                                 |           86.2 %   |         86.2 %  |
| application startup                             |         29616 ms   |       32037 ms  |


*(for complete result data from all 9x9 runs with ehcache disabled, download [result-data-ehcache-disabled.tar.gz](https://glowroot.s3.amazonaws.com/glowroot-benchmark/result-data-ehcache-disabled.tar.gz))*
