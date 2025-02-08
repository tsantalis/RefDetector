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
package org.apache.plc4x.java.api.messages.specific;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.items.WriteRequestItem;
import org.apache.plc4x.java.api.model.Address;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeSafePlcWriteRequestTest {

    WriteRequestItem<String> writeRequestItemString;

    @Before
    public void setUp() {
        writeRequestItemString = new WriteRequestItem<>(String.class, mock(Address.class));
    }

    @Test
    public void constuctor() {
        new TypeSafePlcWriteRequest<>(String.class);
        new TypeSafePlcWriteRequest<>(String.class, mock(PlcWriteRequest.class));
        PlcWriteRequest request = mock(PlcWriteRequest.class);
        when(request.getRequestItems()).thenReturn(Collections.singletonList(writeRequestItemString));
        new TypeSafePlcWriteRequest<>(String.class, request);
        new TypeSafePlcWriteRequest<>(String.class, mock(Address.class));
        new TypeSafePlcWriteRequest<>(String.class, mock(Address.class), "");
        new TypeSafePlcWriteRequest<>(String.class, writeRequestItemString);
        assertThatThrownBy(() -> new TypeSafePlcWriteRequest<>(Byte.class, request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void addItem() {
        new TypeSafePlcWriteRequest<>(String.class).addItem(writeRequestItemString);
    }

    @Test
    public void getCheckedWriteRequestItems() {
        new TypeSafePlcWriteRequest<>(String.class).getCheckedRequestItems();
    }

    @Test
    public void getRequestItem() {
        new TypeSafePlcWriteRequest<>(String.class).getRequestItem();
    }

    @Test
    public void getDataType() {
        assertThat(new TypeSafePlcWriteRequest<>(String.class).getDataType()).isEqualTo(String.class);
    }

}