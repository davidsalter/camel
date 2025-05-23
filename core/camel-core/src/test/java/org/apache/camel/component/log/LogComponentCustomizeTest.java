/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.log;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogComponentCustomizeTest extends ContextTestSupport {

    private final LogCustomFormatterTest.TestExchangeFormatter formatter = new LogCustomFormatterTest.TestExchangeFormatter();

    @Test
    public void testCustomize() throws Exception {
        Assertions.assertEquals(0, formatter.getCounter());

        template.sendBody("direct:start", "Hello World");

        Assertions.assertEquals(1, formatter.getCounter());
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // customize the log component using java lambda style (using the default name)
                customize(LogComponent.class, l -> {
                    l.setExchangeFormatter(formatter);
                });

                from("direct:start")
                        .to("log:foo");
            }
        };
    }
}
