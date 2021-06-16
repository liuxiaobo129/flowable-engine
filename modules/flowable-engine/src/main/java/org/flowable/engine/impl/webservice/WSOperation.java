/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl.webservice;

import java.net.URL;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.flowable.engine.impl.bpmn.webservice.MessageDefinition;
import org.flowable.engine.impl.bpmn.webservice.MessageInstance;
import org.flowable.engine.impl.bpmn.webservice.Operation;
import org.flowable.engine.impl.bpmn.webservice.OperationImplementation;

/**
 * Represents a WS implementation of a {@link Operation}
 * 
 * @author Esteban Robles Luna
 */
public class WSOperation implements OperationImplementation {

    protected String id;

    protected String name;

    protected WSService service;

    public WSOperation(String id, String operationName, WSService service) {
        this.id = id;
        this.name = operationName;
        this.service = service;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public MessageInstance sendFor(MessageInstance message, Operation operation, ConcurrentMap<QName, URL> overridenEndpointAddresses) throws Exception {
        Object[] inArguments = this.getInArguments(message);
        Object[] outArguments = this.getOutArguments(operation);

        // For each out parameters, a value 'null' must be set in arguments passed to the Apache CXF API to invoke
        // web-service operation
        Object[] arguments = new Object[inArguments.length + outArguments.length];
        System.arraycopy(inArguments, 0, arguments, 0, inArguments.length);
        Object[] results = this.safeSend(arguments, overridenEndpointAddresses);
        return this.createResponseMessage(results, operation);
    }

    private Object[] getInArguments(MessageInstance message) {
        return message.getStructureInstance().toArray();
    }

    private Object[] getOutArguments(Operation operation) {

        MessageDefinition outMessage = operation.getOutMessage();
        if (outMessage != null) {
            return outMessage.createInstance().getStructureInstance().toArray();
        } else {
            return new Object[] {};
        }
    }

    private Object[] safeSend(Object[] arguments, ConcurrentMap<QName, URL> overridenEndpointAddresses) throws Exception {
        Object[] results = null;

        results = this.service.getClient().send(this.name, arguments, overridenEndpointAddresses);

        if (results == null) {
            results = new Object[] {};
        }
        return results;
    }

    private MessageInstance createResponseMessage(Object[] results, Operation operation) {
        MessageInstance message = null;
        MessageDefinition outMessage = operation.getOutMessage();
        if (outMessage != null) {
            message = outMessage.createInstance();
            message.getStructureInstance().loadFrom(results);
        }
        return message;
    }

    public WSService getService() {
        return this.service;
    }
}
