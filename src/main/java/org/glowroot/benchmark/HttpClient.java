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
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class HttpClient {

    private static final int LOWERCASE_OFFSET = 'a' - 'A';

    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;
    private final byte[] requestBytes;

    private final byte[] buffer = new byte[8192];

    HttpClient(String host, int port, String path) throws IOException {
        socket = new Socket(host, port);
        out = socket.getOutputStream();
        in = socket.getInputStream();
        String request = "GET " + path + " HTTP/1.1\r\nHost: " + host + ":" + port + "\r\n\r\n";
        requestBytes = request.getBytes();
    }

    void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }

    void execute() throws IOException {
        out.write(requestBytes);
        drain(in, buffer);
    }

    // visible for testing
    static int drain(InputStream in, byte[] buffer) throws IOException {
        int startLookingFromIndex = 0;
        int bytesRead = 0;
        int headerLen = 0;
        List<Integer> possibleHeaderPositions = new ArrayList<Integer>(2);
        while (headerLen == 0) {
            bytesRead += in.read(buffer, bytesRead, buffer.length - bytesRead);
            for (int i = startLookingFromIndex; i < bytesRead - 2; i++) {
                if (buffer[i] == '\r') {
                    int nextLineStartPosition = i + 2;
                    byte c = buffer[nextLineStartPosition];
                    if (c == '\r') {
                        // found end of header
                        headerLen = nextLineStartPosition + 2;
                        break;
                    } else if (c == 'C' || c == 'c') {
                        possibleHeaderPositions.add(nextLineStartPosition);
                    }
                }
            }
            startLookingFromIndex = Math.max(bytesRead - 2, 0);
        }
        int contentLength = getContentLength(buffer, possibleHeaderPositions);
        if (contentLength == 0) {
            throw new IllegalStateException("Did not find Content-Length header");
        }
        while (bytesRead < headerLen + contentLength) {
            bytesRead += in.read(buffer, bytesRead, buffer.length - bytesRead);
        }
        return bytesRead;
    }

    private static int getContentLength(byte[] buffer, List<Integer> possibleHeaderPositions) {
        final byte[] CONTENT_LENGTH = "CONTENT-LENGTH:".getBytes();
        for (int pos : possibleHeaderPositions) {
            boolean match = true;
            for (int j = 0; j < CONTENT_LENGTH.length; j++) {
                byte c = buffer[pos + j];
                byte d = CONTENT_LENGTH[j];
                if (c != d && c != d + LOWERCASE_OFFSET) {
                    match = false;
                    break;
                }
            }
            if (match) {
                StringBuilder sb = new StringBuilder();
                byte c;
                int j = pos + CONTENT_LENGTH.length;
                while ((c = buffer[j++]) != '\r') {
                    sb.append((char) c);
                }
                return Integer.parseInt(sb.toString().trim());
            }
        }
        throw new IllegalStateException("Did not find content-length header");
    }
}
