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
package org.apache.coheigea.misc.saaj;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.net.URL;

import javax.xml.soap.MessageFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SAAJTest {

    @org.junit.Test
    public void testSAAJSignature() throws Exception {

        URL signatureFile = SAAJTest.class.getResource("signature-saaj.xml");
        assertNotNull(signatureFile);

        Document doc = readUsingSAAJ(signatureFile);

        Element refElement = 
            (Element)doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Reference").item(0);

        Attr uriAttr = refElement.getAttributeNodeNS(null, "URI");
        assertEquals(uriAttr.getOwnerDocument(), refElement.getOwnerDocument());
    }

    private static Document readUsingSAAJ(URL file) throws Exception {
        try (InputStream in = file.openStream()) {
            MessageFactory saajFactory = MessageFactory.newInstance();
            return saajFactory.createMessage(null, in).getSOAPPart();
        }
    }
}