/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.benchmark;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class ResultFormatter {

    public static void main(String[] args) throws IOException {
        File resultFile = new File(args[0]);

        Scores scores = new Scores();
        double baselineStartupTime = -1;
        double glowrootStartupTime = -1;

        ObjectMapper mapper = new ObjectMapper();
        String content = Files.toString(resultFile, Charsets.UTF_8);
        content = content.replaceAll("\n", " ");
        ArrayNode results = (ArrayNode) mapper.readTree(content);
        for (JsonNode result : results) {
            String benchmark = result.get("benchmark").asText();
            benchmark = benchmark.substring(0, benchmark.lastIndexOf('.'));
            ObjectNode primaryMetric = (ObjectNode) result.get("primaryMetric");
            double score = primaryMetric.get("score").asDouble();
            if (benchmark.equals(StartupBenchmark.class.getName())) {
                baselineStartupTime = score;
                continue;
            } else if (benchmark.equals(StartupWithGlowrootBenchmark.class.getName())) {
                glowrootStartupTime = score;
                continue;
            }
            ObjectNode scorePercentiles = (ObjectNode) primaryMetric.get("scorePercentiles");
            double score50 = scorePercentiles.get("50.0").asDouble();
            double score95 = scorePercentiles.get("95.0").asDouble();
            double score99 = scorePercentiles.get("99.0").asDouble();
            double score999 = scorePercentiles.get("99.9").asDouble();
            double score9999 = scorePercentiles.get("99.99").asDouble();
            double allocatedBytes = getAllocatedBytes(result);
            if (benchmark.equals(ServletBenchmark.class.getName())) {
                scores.baselineResponseTimeAvg = score;
                scores.baselineResponseTime50 = score50;
                scores.baselineResponseTime95 = score95;
                scores.baselineResponseTime99 = score99;
                scores.baselineResponseTime999 = score999;
                scores.baselineResponseTime9999 = score9999;
                scores.baselineAllocatedBytes = allocatedBytes;
            } else if (benchmark.equals(ServletWithGlowrootBenchmark.class.getName())) {
                scores.glowrootResponseTimeAvg = score;
                scores.glowrootResponseTime50 = score50;
                scores.glowrootResponseTime95 = score95;
                scores.glowrootResponseTime99 = score99;
                scores.glowrootResponseTime999 = score999;
                scores.glowrootResponseTime9999 = score9999;
                scores.glowrootAllocatedBytes = allocatedBytes;
            } else {
                throw new AssertionError(benchmark);
            }
        }
        System.out.println();
        printScores(scores);
        if (baselineStartupTime != -1) {
            System.out.println("STARTUP");
            System.out.format("%25s%14s%14s%n", "", "baseline", "glowroot");
            printLine("Startup time (avg)", "ms", baselineStartupTime, glowrootStartupTime);
        }
        System.out.println();
    }

    private static double getAllocatedBytes(JsonNode result) {
        ObjectNode secondaryMetrics = (ObjectNode) result.get("secondaryMetrics");
        if (secondaryMetrics == null) {
            return -1;
        }
        ObjectNode edenSpaceChurnNormalized =
                (ObjectNode) secondaryMetrics.get("\u00b7gc.churn.PS_Eden_Space.norm");
        if (edenSpaceChurnNormalized == null) {
            return -1;
        }
        return edenSpaceChurnNormalized.get("score").asDouble();
    }

    private static void printScores(Scores scores) {
        System.out.format("%25s%14s%14s%n", "", "baseline", "glowroot");
        printLine("Response time (avg)", "us", scores.baselineResponseTimeAvg,
                scores.glowrootResponseTimeAvg);
        printLine("Response time (50th)", "us", scores.baselineResponseTime50,
                scores.glowrootResponseTime50);
        printLine("Response time (95th)", "us", scores.baselineResponseTime95,
                scores.glowrootResponseTime95);
        printLine("Response time (99th)", "us", scores.baselineResponseTime99,
                scores.glowrootResponseTime99);
        printLine("Response time (99.9th)", "us", scores.baselineResponseTime999,
                scores.glowrootResponseTime999);
        printLine("Response time (99.99th)", "us", scores.baselineResponseTime9999,
                scores.glowrootResponseTime9999);
        if (scores.baselineAllocatedBytes != -1 && scores.glowrootAllocatedBytes != -1) {
            printLine("Allocation memory per req", "kb", scores.baselineAllocatedBytes / 1024.0,
                    scores.glowrootAllocatedBytes / 1024.0);
        }
        System.out.println();
    }

    private static void printLine(String label, String unit, double baselineScore,
            double glowrootScore) {
        System.out.format("%-25s %10.3f %s %10.3f %s%n", label, baselineScore, unit, glowrootScore,
                unit);
    }

    private static class Scores {
        private double baselineResponseTimeAvg;
        private double baselineResponseTime50;
        private double baselineResponseTime95;
        private double baselineResponseTime99;
        private double baselineResponseTime999;
        private double baselineResponseTime9999;
        private double baselineAllocatedBytes;
        private double glowrootResponseTimeAvg;
        private double glowrootResponseTime50;
        private double glowrootResponseTime95;
        private double glowrootResponseTime99;
        private double glowrootResponseTime999;
        private double glowrootResponseTime9999;
        private double glowrootAllocatedBytes;
    }
}
