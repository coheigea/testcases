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

package org.apache.coheigea.cxf.sts.xacml.authorization.xacml3;

import java.security.Principal;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.openaz.xacml.api.Request;

/**
 * This interface defines a way to create an XACML 3.0 Request using OpenAZ
 */
public interface XACML3RequestBuilder {
    
    /**
     * Create an XACML Request given a Principal, list of roles and Message.
     * 
     * @param principal The principal to insert into the Subject of the Request
     * @param roles The list of roles associated with the principal
     * @param message The Message from which to retrieve the resource
     * @return An OpenAZ Request object
     * @throws Exception
     */
    Request createRequest(Principal principal, List<String> roles, Message message) throws Exception;
    
}