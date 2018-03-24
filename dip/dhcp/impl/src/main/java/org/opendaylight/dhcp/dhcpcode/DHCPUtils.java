/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.dhcpcode;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public abstract class DHCPUtils {

    public static byte[] byteToByteArray(byte b) {
        return new byte[] {b};
    }

    public static byte[] shortToByteArray(short s) {
        return new byte[] { (byte) ((s >> 8) & 0xff), (byte) (s & 0xff) };
    }

    public static byte[] intToByteArray(int i ) {
        return new byte[] { (byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff),
                        (byte) (i & 0xff) };
    }

    public static byte[] longToByteArray(long i ) {
        return new byte[] {(byte) ((i >> 56) & 0xff),(byte) ((i >> 48) & 0xff),(byte) ((i >> 40) & 0xff),
                (byte) ((i >> 32) & 0xff), (byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff),
                (byte) (i & 0xff) };
    }

    public static byte[] inetAddrToByteArray(InetAddress a) {
        return a.getAddress();
    }

    public static byte[] strAddrToByteArray(String addr) {
        try {
            return InetAddress.getByName(addr).getAddress();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] strListAddrsToByteArray(List<String> strList) {
        byte[] result = new byte[strList.size() * 4];
        byte[] addr = new byte[4];
        try {
        for (int i = 0; i < strList.size(); i++) {
                    addr = InetAddress.getByName(strList.get(i)).getAddress();
                System.arraycopy(addr, 0, result, i*4, 4);
        }
        } catch (UnknownHostException e) {
            return null;
        }
        return result;
    }

    public static short byteArrayToShort(byte[] ba) {
        if (ba == null || ba.length != 2) {
            return 0;
        }
        return (short) ((0xff & ba[0]) << 8 | (0xff & ba[1]));
    }

    public static InetAddress byteArrayToInetAddr(byte[] ba) {
        try {
            return InetAddress.getByAddress(ba);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static byte[] strMacAddrtoByteArray(String macAddress) {
        if(macAddress == null) {
            return null;
        }
        String[] bytes = macAddress.split(":");
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            BigInteger temp = new BigInteger(bytes[i], 16);
            byte[] raw = temp.toByteArray();
            result[i] = raw[raw.length - 1];
        }
        return result;
    }

}
