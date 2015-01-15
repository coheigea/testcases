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
package org.apache.coheigea.camel.spring_ldap;

import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttributes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * A Camel processor that takes the results of the LDAP Search (a List<BasicAttributes>),
 * and writes out the search result name and attributes to the Message Body instead
 */
public class SpringLDAPSearchProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
        List<?> data = exchange.getIn().getBody(List.class); 
        StringBuilder result = new StringBuilder();
        for (Object obj : data) {
            if (obj instanceof BasicAttributes) {
                BasicAttributes attributes = (BasicAttributes)obj;
                
                // Write out the attributes
                NamingEnumeration<? extends Attribute> attrs = attributes.getAll();
                while (attrs.hasMore()) {
                    Attribute attribute = attrs.next();
                    // Don't write out the password or objectclass
                    if (!"userpassword".equals(attribute.getID()) 
                        && !"objectclass".equals(attribute.getID())) {
                        result.append(attribute.toString() + "\n");
                    }
                }
            }
        }
        exchange.getIn().setBody(result.toString());
    }
    
}
