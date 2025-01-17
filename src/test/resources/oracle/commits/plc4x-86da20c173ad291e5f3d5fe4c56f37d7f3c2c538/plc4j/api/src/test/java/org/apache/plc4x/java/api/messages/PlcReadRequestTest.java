/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package org.apache.plc4x.java.api.messages;

import org.apache.plc4x.java.api.messages.items.ReadRequestItem;
import org.apache.plc4x.java.api.model.Address;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class PlcReadRequestTest {

    Address dummyAddress;

    @Before
    public void setUp() {
        dummyAddress = mock(Address.class);
    }

    @Test
    public void constuctor() {
        new PlcReadRequest();
        new PlcReadRequest(new ReadRequestItem<>(String.class, dummyAddress));
        new PlcReadRequest(String.class, dummyAddress);
        new PlcReadRequest(String.class, dummyAddress, 13);
        new PlcReadRequest(Collections.singletonList(new ReadRequestItem<>(String.class, dummyAddress)));
    }

    @Test
    public void builder() {
        { // empty
            assertThatThrownBy(() ->
                PlcReadRequest.builder().build())
                .isInstanceOf(IllegalStateException.class);
        }
        { // one item
            PlcReadRequest.builder()
                .addItem(String.class, dummyAddress)
                .build();
        }
        { // one item sized
            PlcReadRequest.builder()
                .addItem(String.class, dummyAddress, 13)
                .build();
        }
        { // one item prebuild
            PlcReadRequest.builder()
                .addItem(new ReadRequestItem<>(String.class, dummyAddress))
                .build();
        }
        { // two different item
            PlcReadRequest.builder()
                .addItem(String.class, dummyAddress)
                .addItem(Byte.class, dummyAddress)
                .build();
        }
        { // two different item typeSafe
            assertThatThrownBy(() ->
                PlcReadRequest.builder()
                    .addItem(String.class, dummyAddress)
                    .addItem(Byte.class, dummyAddress)
                    .build(String.class))
                .isInstanceOf(IllegalStateException.class);
        }
        { // two different item typeSafe
            assertThatThrownBy(() ->
                PlcReadRequest.builder()
                    .addItem(String.class, dummyAddress)
                    .addItem(Byte.class, dummyAddress)
                    .build(Byte.class))
                .isInstanceOf(ClassCastException.class);
        }
        { // two equal item typeSafe
            PlcReadRequest.builder()
                .addItem(Byte.class, dummyAddress)
                .addItem(Byte.class, dummyAddress)
                .build(Byte.class);
        }
    }

}