/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.coheigea.serializer.performance;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceTimingTest extends AbstractPerformanceTest {

    private static final int runs = 40;
    private static final int xmlResizeFactor = 1000;

    private static Map<Integer, File> encryptedFiles = new TreeMap<>();


    @Override
    protected File getTmpFilePath() {
        return new File("target/performanceTimingTest");
    }

    @Order(1)
    @Test
    public void testRunFirstOutboundEncryptionTimePerformance() throws Exception {
        System.out.println("Testing Outbound Encryption Time Performance");
        FileWriter outEncryptionSamplesWriter = new FileWriter("target/encryptionOutTimeSamples.txt", false);
        for (int i = 1; i <= runs; i++) {
            System.out.println("Run " + i);

            File file = generateLargeXMLFile(i * xmlResizeFactor);

            int startTagCount = countXMLStartTags(file);
            outEncryptionSamplesWriter.write("" + startTagCount);

            long start = System.currentTimeMillis();
            File encryptedFile = doStreamingEncryptionOutbound(file, startTagCount);
            outEncryptionSamplesWriter.write(" " + ((System.currentTimeMillis() - start) / 1000.0));
            encryptedFiles.put(startTagCount, encryptedFile);
            doGC();

            start = System.currentTimeMillis();
            doDOMEncryptionOutbound(file, startTagCount);
            outEncryptionSamplesWriter.write(" " + ((System.currentTimeMillis() - start) / 1000.0));
            doGC();

            outEncryptionSamplesWriter.write("\n");
        }
        outEncryptionSamplesWriter.close();
    }

    @Order(2)
    @Test
    public void testRunSecondInboundDecryptionTimePerformance() throws Exception {
        System.out.println("Testing Inbound Decryption Time Performance");
        FileWriter inEncryptionSamplesWriter = new FileWriter("target/encryptionInTimeSamples.txt", false);

        int run = 1;
        Iterator<Map.Entry<Integer, File>> mapIterator = encryptedFiles.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<Integer, File> entry = mapIterator.next();
            System.out.println("Run " + (run++));

            File file = entry.getValue();
            Integer startTagCount = entry.getKey();
            inEncryptionSamplesWriter.write("" + startTagCount);

            long start = System.currentTimeMillis();
            doStreamingDecryptionInbound(file, startTagCount);
            inEncryptionSamplesWriter.write(" " + ((System.currentTimeMillis() - start) / 1000.0));
            doGC();

            start = System.currentTimeMillis();
            doDOMDecryptionInbound(file, startTagCount);
            inEncryptionSamplesWriter.write(" " + ((System.currentTimeMillis() - start) / 1000.0));
            doGC();

            inEncryptionSamplesWriter.write("\n");
        }
        inEncryptionSamplesWriter.close();
    }

    private void doGC() {
        Runtime.getRuntime().runFinalization();
        System.gc();
        System.gc();
    }
}
