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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.example.helloworld.HelloWorldApplication;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class ServletBenchmark {

    static {
        try {
            HelloWorldApplication.main(new String[] {"server", "example.yml"});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClient httpClient;

    @Setup
    public void setup() throws Exception {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
                .setAllowPoolingConnections(true)
                .build();
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient(config);
        asyncHttpClient.preparePost("http://localhost:8080/people")
                .setHeader("Content-Type", "application/json")
                .setBody("{\"firstName\":\"abc\",\"lastName\":\"xyz\"}")
                .execute().get();
        asyncHttpClient.close();
        httpClient = new HttpClient("localhost", 8080, "/people/1");
    }

    @TearDown
    public void tearDown() throws IOException {
        httpClient.close();
    }

    @Benchmark
    public void execute() throws Exception {
        httpClient.execute();
    }
}
