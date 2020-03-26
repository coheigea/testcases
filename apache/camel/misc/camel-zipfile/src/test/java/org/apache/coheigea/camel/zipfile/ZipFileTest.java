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
package org.apache.coheigea.camel.zipfile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
public class ZipFileTest {

    @Test
    public void testPlaintextRoute() throws IOException, InterruptedException {
        Path sourcePath = Paths.get("src/test/resources/data");
        Path destPath = Paths.get("target/plaintextdata");
        FileSystemUtils.copyRecursively(sourcePath, destPath);

        // Sleep to let the processing take place
        Thread.sleep(2000);
    }

    @Test
    public void testZippedRoute() throws IOException, InterruptedException {
        Path sourcePath = Paths.get("src/test/resources/zippeddata");
        Path destPath = Paths.get("target/zippeddata");
        FileSystemUtils.copyRecursively(sourcePath, destPath);

        // Sleep to let the processing take place
        Thread.sleep(2000);
    }

}