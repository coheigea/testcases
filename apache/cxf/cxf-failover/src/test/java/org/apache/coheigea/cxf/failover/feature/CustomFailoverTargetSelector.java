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
package org.apache.coheigea.cxf.failover.feature;

import java.net.SocketTimeoutException;

import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

public class CustomFailoverTargetSelector extends FailoverTargetSelector {


    @Override
    protected boolean requiresFailover(Exchange exchange, Exception ex) {
        Throwable curr = ex;
        boolean failover = false;
        while (curr != null) {
            failover = curr instanceof java.io.IOException && !(curr instanceof SocketTimeoutException);
            curr = curr.getCause();
        }

        if (isSupportNotAvailableErrorsOnly() && exchange.get(Message.RESPONSE_CODE) != null) {
            failover = PropertyUtils.isTrue(exchange.get("org.apache.cxf.transport.service_not_available"));
        }
        
        System.out.println("Failing over: " + failover);

        return failover;
    }
}
