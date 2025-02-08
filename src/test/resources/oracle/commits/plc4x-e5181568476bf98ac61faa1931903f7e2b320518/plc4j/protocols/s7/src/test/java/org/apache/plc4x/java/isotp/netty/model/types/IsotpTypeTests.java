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

package org.apache.plc4x.java.isotp.netty.model.types;

import org.apache.plc4x.java.api.exceptions.PlcProtocolException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

class IsotpTypeTests {

    @Test
    @Tag("fast")
    void deviceGroup() {
        DeviceGroup deviceGroup;

        deviceGroup = DeviceGroup.PG_OR_PC;
        assertTrue(deviceGroup.getCode() == (byte)1, "code is not 1");

        deviceGroup = DeviceGroup.OS;
        assertTrue(deviceGroup.getCode() == (byte)2, "code is not 2");

        deviceGroup = DeviceGroup.OTHERS;
        assertTrue(deviceGroup.getCode() == (byte)3, "code is not 3");
    }

    @Test
    @Tag("fast")
    void deviceGroupUnknown() {
        DeviceGroup deviceGroup = DeviceGroup.valueOf((byte)0x40);

        assertNull(deviceGroup, "expected device group to be null");
    }


    @Test
    @Tag("fast")
    void disconnectReason() {
        DisconnectReason disconnectReason = DisconnectReason.ADDRESS_UNKNOWN;

        assertTrue(DisconnectReason.valueOf((byte)3) == DisconnectReason.ADDRESS_UNKNOWN, "3 incorrectly mapped");
        assertTrue(disconnectReason.getCode() == (byte)3, "code is not 3");
    }

    @Test
    @Tag("fast")
    void diosconectReasonUnknown() {
        DisconnectReason disconnectReason = DisconnectReason.valueOf((byte)4);

        assertNull(disconnectReason, "expected disconnect reason to be null");
    }

    @Test
    @Tag("fast")
    void parameterCode() {
        ParameterCode parameterCode = ParameterCode.CALLING_TSAP;

        assertTrue(ParameterCode.valueOf((byte)0xC1) == ParameterCode.CALLING_TSAP, "0xC1 incorrectly mapped");
        assertTrue(parameterCode.getCode() == (byte)0xC1, "code is not 0xC1");
    }

    @Test
    @Tag("fast")
    void parameterCodeUnknown() {
        ParameterCode parameterCode = ParameterCode.valueOf((byte)0x90);

        assertNull(parameterCode, "expected parameter code to be null");
    }

    @Test
    @Tag("fast")
    void protocolClass() {
        ProtocolClass protocolClass;

        protocolClass = ProtocolClass.CLASS_1;
        assertTrue(protocolClass.getCode() == (byte)0x10, "code is not 0x10");

        protocolClass = ProtocolClass.CLASS_2;
        assertTrue(protocolClass.getCode() == (byte)0x20, "code is not 0x20");

        protocolClass = ProtocolClass.CLASS_3;
        assertTrue(protocolClass.getCode() == (byte)0x30, "code is not 0x30");

        protocolClass = ProtocolClass.CLASS_4;
        assertTrue(protocolClass.getCode() == (byte)0x40, "code is not 0x40");
    }

    @Test
    @Tag("fast")
    void protocolClassUnknown() {
        ProtocolClass protocolClass = ProtocolClass.valueOf((byte)0x50);

        assertNull(protocolClass, "expected protocol class to be null");
    }

    @Test
    @Tag("fast")
    void rejectCause() {
        RejectCause rejectCause = RejectCause.INVALID_PARAMETER_TYPE;

        assertTrue(RejectCause.valueOf((byte)0x03) == RejectCause.INVALID_PARAMETER_TYPE, "0x03 incorrectly mapped");
        assertTrue(rejectCause.getCode() == (byte)0x03, "code is not 0x03");
    }

    @Test
    @Tag("fast")
    void rejectClauseUnknown() {
        RejectCause rejectCause = RejectCause.valueOf((byte)0x90);

        assertNull(rejectCause, "expected reject cause to be null");
    }

    @Test
    @Tag("fast")
    void tpduCode() {
        TpduCode tpduCode = TpduCode.DATA;

        assertTrue(TpduCode.valueOf((byte)0xF0) == TpduCode.DATA, "0xF0 incorrectly mapped");
        assertTrue(tpduCode.getCode() == (byte)0xF0, "code is not 0xF0");
    }

    @Test
    @Tag("fast")
    void tpduCodeUnknown() {
        TpduCode tpduCode = TpduCode.valueOf((byte)0x01);

        assertTrue(TpduCode.valueOf((byte)0xFF) == TpduCode.TPDU_UNKNOWN, "0xFF incorrectly mapped");
        assertTrue(tpduCode.getCode() == (byte)0xFF, "code is not 0xFF");
    }
    
    @Test
    @Tag("fast")
    void typduSize() {
        TpduSize tpduSize = TpduSize.SIZE_128;

        assertTrue(TpduSize.valueOf((byte)0x07) == TpduSize.SIZE_128, "0x07 incorrectly mapped");
        assertTrue(tpduSize.getCode() == (byte)0x07, "code is not 0x07");
        assertEquals(tpduSize.getValue(), 128, "the value is not 128");
    }

    @Test
    @Tag("fast")
    void tpduSizeUnknown() {
        TpduSize tpduSize = TpduSize.valueOf((byte)0x06);

        assertNull(tpduSize, "expected tpdu size to be null");
    }

    /**
     * If we are requesting exactly the size of one of the iso tp
     * pdu sizes, then exactly that box should be returned.
     */
    @Test
    @Tag("fast")
    void tpduValueForGivenExactFit() {
        TpduSize tpduSize = TpduSize.valueForGivenSize(256);

        assertEquals(TpduSize.SIZE_256, tpduSize, "expected tpdu size of 256");
    }

    /**
     * In this case we have a given value that is in-between the boundaries of
     * a pdu box, the method should return the next larger box.
     */
    @Test
    @Tag("fast")
    void tpduValueForGivenIntermediateSize() {
        TpduSize tpduSize = TpduSize.valueForGivenSize(222);

        assertEquals(TpduSize.SIZE_256, tpduSize, "expected tpdu size of 256");
        assertNotEquals(222, tpduSize.getValue(), "the value is not 222");
    }

    /**
     * This test should cause an exception as the tpdu size has to be greater
     * than 0 in any case.
     */
    @Test
    @Tag("fast")
    void tpduValueForGivenTooSmallSize() {
        Executable closureContainingCodeToTest = () -> TpduSize.valueForGivenSize(-1);

        assertThrows(IllegalArgumentException.class, closureContainingCodeToTest,
            "An exception should have been thrown.");
    }

    /**
     * In this test the tpdu size is greater than the maximum defined by the iso tp
     * protocol spec, so it is automatically downgraded to the maximum valid value.
     */
    @Test
    @Tag("fast")
    void tpduValueForGivenTooGreatSize() {
        TpduSize tpduSize = TpduSize.valueForGivenSize(10000);

        assertEquals(TpduSize.SIZE_8192, tpduSize, "expected tpdu size of 8192");
    }

}