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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceMemoryTest extends AbstractPerformanceTest {

    private static final int runs = 40;
    private static final int xmlResizeFactor = 1000;

    //junit creates for every test method a new class instance so we need a static list
    private static Map<Integer, File> encryptedFiles = new TreeMap<>();


    @Override
    protected File getTmpFilePath() {
        return new File("target/performanceMemoryTest");
    }

    @Order(1)
    @Test
    public void testRunFirstOutboundEncryptionMemoryPerformance() throws Exception {
        System.out.println("Testing Outbound Encryption Memory Performance");
        FileWriter outEncryptionSamplesWriter = new FileWriter("target/encryptionOutMemorySamples.txt", false);
        for (int i = 1; i <= runs; i++) {
            System.out.println("Run " + i);

            File file = generateLargeXMLFile(i * xmlResizeFactor);

            int startTagCount = countXMLStartTags(file);
            outEncryptionSamplesWriter.write("" + startTagCount);

            long startMem = getUsedMemory();
            MemorySamplerThread mst = new MemorySamplerThread(startMem);
            Thread thread = new Thread(mst);
            thread.setPriority(9);
            thread.start();
            File encryptedFile = doStreamingEncryptionOutbound(file, startTagCount);
            mst.setStop(true);
            thread.join();
            outEncryptionSamplesWriter.write(" " + mst.getMaxUsedMemory());
            encryptedFiles.put(startTagCount, encryptedFile);

            startMem = getUsedMemory();
            mst = new MemorySamplerThread(startMem);
            thread = new Thread(mst);
            thread.setPriority(9);
            thread.start();
            doDOMEncryptionOutbound(file, startTagCount);
            mst.setStop(true);
            thread.join();
            outEncryptionSamplesWriter.write(" " + mst.getMaxUsedMemory());

            outEncryptionSamplesWriter.write("\n");
        }
        outEncryptionSamplesWriter.close();
    }

    @Order(2)
    @Test
    public void testRunSecondInboundDecryptionMemoryPerformance() throws Exception {
        System.out.println("Testing Inbound Decryption Memory Performance");
        FileWriter inEncryptionSamplesWriter = new FileWriter("target/encryptionInMemorySamples.txt", false);

        int run = 1;
        Iterator<Map.Entry<Integer, File>> mapIterator = encryptedFiles.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<Integer, File> entry = mapIterator.next();
            System.out.println("Run " + (run++));

            File file = entry.getValue();
            Integer startTagCount = entry.getKey();
            inEncryptionSamplesWriter.write("" + startTagCount);

            long startMem = getUsedMemory();
            MemorySamplerThread mst = new MemorySamplerThread(startMem);
            Thread thread = new Thread(mst);
            thread.setPriority(9);
            thread.start();
            doStreamingDecryptionInbound(file, startTagCount);
            mst.setStop(true);
            thread.join();
            inEncryptionSamplesWriter.write(" " + mst.getMaxUsedMemory());

            startMem = getUsedMemory();
            mst = new MemorySamplerThread(startMem);
            thread = new Thread(mst);
            thread.setPriority(9);
            thread.start();
            doDOMDecryptionInbound(file, startTagCount);
            inEncryptionSamplesWriter.write(" " + mst.getMaxUsedMemory());
            mst.setStop(true);
            thread.join();

            inEncryptionSamplesWriter.write("\n");
        }
        inEncryptionSamplesWriter.close();
    }

    private static void gc() {
        System.gc();
        System.runFinalization();
        System.gc();
    }

    private static long getUsedMemory() {
        gc();
        gc();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return totalMemory - freeMemory;
    }

    class MemorySamplerThread implements Runnable {

        private long memoryDiff;
        private volatile boolean stop = false;

        private List<Integer> memory = new LinkedList<>();

        MemorySamplerThread(long memoryDiff) {
            this.memoryDiff = memoryDiff;
            System.out.println("memory diff " + memoryDiff / 1024 / 1024);
        }

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run() {
            int sleepTime = 50;
            while (!isStop()) {
                try {
                    Thread.sleep(sleepTime);
                    if (isStop()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                memory.add((int)((getUsedMemory() - memoryDiff) / 1024.0 / 1024.0));
            }
        }

        public int getMaxUsedMemory() {
            System.out.println("Collected " + memory.size() + " samples");
            int maxMem = Integer.MIN_VALUE;
            for (int i = 0; i < memory.size(); i++) {
                int mem = memory.get(i);
                maxMem = mem > maxMem ? mem : maxMem;
            }
            System.out.println("Max memory usage: " + maxMem + "MB");

            if (maxMem > Integer.MIN_VALUE) {
                return maxMem;
            }
            return 0;
        }
    }
}
