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
package org.coheigea.misc.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

public class JsonTest {

    @org.junit.Test
    public void testLoadAndParseJson() throws Exception {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data.json");
        
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
        assertEquals("Colm", data.get("name"));
    }
    
    @org.junit.Test
    public void testLoadAndParseCustomObject() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        CustomObject sendObject = new CustomObject();
        sendObject.setFoo("bar");
        objectMapper.writeValue(sw, sendObject);
        
        CustomObject receivedObject = objectMapper.readValue(new StringReader(sw.toString()), CustomObject.class);
        assertEquals("bar", receivedObject.getFoo());
    }
    
    // Ref: https://medium.com/@cowtowncoder/on-jackson-cves-dont-panic-here-is-what-you-need-to-know-54cd0d6e8062
    @org.junit.Test
    public void testPolymorphicDeserialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        PolyObject sendObject = new PolyObject();
        CustomObject customObject = new CustomObject();
        customObject.setFoo("bar");
        sendObject.setCustomObject(customObject);
        objectMapper.writeValue(sw, sendObject);
        
        PolyObject receivedObject = objectMapper.readValue(new StringReader(sw.toString()), PolyObject.class);
        assertNotNull(receivedObject);
        assertTrue(receivedObject.getCustomObject() instanceof CustomObject);
    }
    
    @org.junit.Test
    public void testPolymorphicDeserializationFromJson() throws Exception {
        
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data-poly.json");
        
        ObjectMapper objectMapper = new ObjectMapper();
        PolyObject receivedObject = objectMapper.readValue(inputStream, PolyObject.class);
        assertTrue(receivedObject.getCustomObject() instanceof CustomClass);
    }
    
    @org.junit.Test
    public void testPolymorphicDeserializationFromJsonSafe() throws Exception {
        
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data-poly.json");
        
        PolymorphicTypeValidator ptv = 
            BasicPolymorphicTypeValidator.builder().allowIfSubType(CustomClass.class).build();
        ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
        PolyObject receivedObject = objectMapper.readValue(inputStream, PolyObject.class);
        assertTrue(receivedObject.getCustomObject() instanceof CustomClass);
    }
    
    @org.junit.Test
    public void testPolymorphicDeserializationFromJsonSafe2() throws Exception {
        
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("data-poly.json");
        
        PolymorphicTypeValidator ptv = 
            BasicPolymorphicTypeValidator.builder().allowIfSubType(CustomObject.class).build();
        ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
        try {
            objectMapper.readValue(inputStream, PolyObject.class);
            fail("Failure expected");
        } catch (Exception ex) {
            // expected
        }
    }


    private static class CustomObject implements Serializable {
        private static final long serialVersionUID = -7249059185723713380L;
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
        
    }
    
    private static class PolyObject {
        @JsonTypeInfo(use = Id.CLASS)
        private Object customObject;

        public Object getCustomObject() {
            return customObject;
        }

        public void setCustomObject(Object customObject) {
            this.customObject = customObject;
        }

        
    }
}