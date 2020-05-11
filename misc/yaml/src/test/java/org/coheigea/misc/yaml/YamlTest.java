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
package org.coheigea.misc.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

public class YamlTest {

    @org.junit.Test
    public void testLoadAndParseYaml() throws Exception {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data.yaml");
        Map<String, String> data = yaml.load(inputStream);
        assertEquals("Colm", data.get("name"));
    }
    
    @org.junit.Test
    public void testLoadAndParseYamlAtAPILevel() throws Exception {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data.yaml");
        Constructor constructor = new CustomConstructor();
        Composer composer = new Composer(new ParserImpl(new StreamReader(new UnicodeReader(inputStream))), new Resolver());
        constructor.setComposer(composer);
        Map<String, String> data = (Map<String, String>)constructor.getSingleData(Map.class);
        assertEquals("Colm", data.get("name"));
    }
    
    @org.junit.Test
    public void testDenialOfServiceAttack() throws Exception {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data-dos.yaml");
        try {
            Map<String, String> data = yaml.load(inputStream);
            assertEquals("Colm", data.get("name"));
            fail("Failure expected on a DoS attack");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("exceeds the specified"));
        }
    }
    
    @org.junit.Test
    public void testDenialOfServiceAttackAtAPILevel() throws Exception {
        // Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data-dos.yaml");
        try {
            // Map<String, String> data = yaml.load(inputStream);
            Constructor constructor = new CustomConstructor();
            Composer composer = new CustomComposer(new ParserImpl(new StreamReader(new UnicodeReader(inputStream))), new Resolver());
            constructor.setComposer(composer);
            Map<String, String> data = (Map<String, String>)constructor.getSingleData(Map.class);
            
            assertEquals("Colm", data.get("name"));
            fail("Failure expected on a DoS attack");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("exceeds the specified"));
        }
    }
    
    @org.junit.Test
    public void referencesWithRecursiveKeysNotAllowedByDefault() {
        String output = createDump(30);
        // Load
        LoaderOptions settings = new LoaderOptions();
        settings.setMaxAliasesForCollections(150);
        Yaml yaml = new Yaml(settings);
        try {
            yaml.load(output);
            fail();
        } catch (Exception e) {
            assertEquals("Recursive key for mapping is detected but it is not configured to be allowed.", e.getMessage());
        }
    }
    
    @org.junit.Test
    public void referencesWithRecursiveKeysNotAllowedByDefaultAtAPILevel() {
        String output = createDump(30);
        // Load
        LoaderOptions settings = new LoaderOptions();
        settings.setMaxAliasesForCollections(150);
        Yaml yaml = new Yaml(new CustomConstructor(), new Representer(), new DumperOptions(), settings);
        try {
            yaml.load(output);
            fail();
        } catch (Exception e) {
            assertEquals("Recursive key for mapping is detected but it is not configured to be allowed.", e.getMessage());
        }
    }
    
    // Take from SnakeYaml test code
    private String createDump(int size) {
        HashMap root = new HashMap();
        HashMap s1, s2, t1, t2;
        s1 = root;
        s2 = new HashMap();
        /*
        the time to parse grows very quickly
        SIZE -> time to parse in seconds
        25 -> 1
        26 -> 2
        27 -> 3
        28 -> 8
        29 -> 13
        30 -> 28
        31 -> 52
        32 -> 113
        33 -> 245
        34 -> 500
         */
        for (int i = 0; i < size; i++) {

            t1 = new HashMap();
            t2 = new HashMap();
            t1.put("foo", "1");
            t2.put("bar", "2");

            s1.put("a", t1);
            s1.put("b", t2);
            s2.put("a", t1);
            s2.put("b", t2);

            s1 = t1;
            s2 = t2;
        }

        // this is VERY BAD code
        // the map has itself as a key (no idea why it may be used except of a DoS attack)
        HashMap f = new HashMap();
        f.put(f, "a");
        f.put("g", root);

        Yaml yaml = new Yaml(new SafeConstructor());
        String output = yaml.dump(f);
        return output;
    }
    
}