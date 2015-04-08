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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpClientTest {

    @Test
    public void testCombinations() throws IOException {
        byte[] buffer = new byte[8192];
        String response = "HTTP/1.1 200 OK\r\nContent-Length: 12\r\n\r\n123456789012";
        for (int i = 1; i < response.length() - 1; i++) {
            String part1 = response.substring(0, i);
            String part2 = response.substring(i);
            List<byte[]> chunks = new ArrayList<byte[]>();
            chunks.add(part1.getBytes());
            chunks.add(part2.getBytes());
            InputStream in = new TestInputStream(chunks);
            HttpClient.drain(in, buffer);
            String string = new String(buffer, 0, response.length());
            assertThat(string).isEqualTo(response);
        }
    }

    private static class TestInputStream extends InputStream {

        private final Iterator<byte[]> chunks;

        private TestInputStream(List<byte[]> chunks) {
            this.chunks = chunks.iterator();
        }

        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            if (!chunks.hasNext()) {
                return -1;
            }
            byte[] next = chunks.next();
            System.arraycopy(next, 0, buffer, off, next.length);
            return next.length;
        }
    }
}
