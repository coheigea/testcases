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

package org.apache.coheigea.cxf.dtd;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import static org.junit.Assert.assertNotNull;

public class WoodstoxDTDTest extends org.junit.Assert {

    @org.junit.Test
    public void testDTDs() throws Exception {
        
    	InputStream signatureFile = loadInputStream(this.getClass().getClassLoader(), "signature.xml");
    	assertNotNull(signatureFile);
    	
    	XMLInputFactory factory = XMLInputFactory.newInstance();
    	factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
    	factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
    	factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
    	factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
        
    	XMLStreamReader reader = factory.createXMLStreamReader(signatureFile, "UTF-8");
    	
    	int event = reader.getEventType();

        while (reader.hasNext()) {
        	if (event == XMLStreamConstants.DTD) {
        		System.out.println("Reader: " + reader.getText());
        	}
        	
        	event = reader.next();
        }
    }
    
    private static InputStream loadInputStream(ClassLoader loader, String location) 
        throws IOException {
        InputStream is = null;
        if (location != null) {
            is = loader.getResourceAsStream(location);
    
            //
            // If we don't find it, then look on the file system.
            //
            if (is == null) {
                is = new FileInputStream(location);
            }
        }
        return is;
    }
    
}