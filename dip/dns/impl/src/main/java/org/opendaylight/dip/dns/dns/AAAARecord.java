/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.dns;

import java.io.*;
import java.net.*;

/**
 * IPv6 Address Record - maps a domain name to an IPv6 address
 *
 * @author Brian Wellington
 */

public class AAAARecord extends Record {

private static final long serialVersionUID = -4588601512069748050L;

private byte [] address;

AAAARecord() {}

@Override
Record
getObject() {
	return new AAAARecord();
}

/**
 * Creates an AAAA Record from the given data
 * @param address The address suffix
 */
public
AAAARecord(Name name, int dclass, long ttl, InetAddress address) {
	super(name, Type.AAAA, dclass, ttl);
	if (Address.familyOf(address) != Address.IPv6) {
        throw new IllegalArgumentException("invalid IPv6 address");
    }
	this.address = address.getAddress();
}

@Override
void
rrFromWire(DNSInput in) throws IOException {
	address = in.readByteArray(16);
}

@Override
void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	address = st.getAddressBytes(Address.IPv6);
}

/** Converts rdata to a String */
@Override
String
rrToString() {
	InetAddress addr;
	try {
		addr = InetAddress.getByAddress(null, address);
	} catch (UnknownHostException e) {
		return null;
	}
	if (addr.getAddress().length == 4) {
		// Deal with Java's broken handling of mapped IPv4 addresses.
		StringBuffer sb = new StringBuffer("0:0:0:0:0:ffff:");
		int high = ((address[12] & 0xFF) << 8) + (address[13] & 0xFF);
		int low = ((address[14] & 0xFF) << 8) + (address[15] & 0xFF);
		sb.append(Integer.toHexString(high));
		sb.append(':');
		sb.append(Integer.toHexString(low));
		return sb.toString();
	}
	return addr.getHostAddress();
}

/** Returns the address */
public InetAddress
getAddress() {
	try {
		if (name == null) {
            return InetAddress.getByAddress(address);
        } else {
            return InetAddress.getByAddress(name.toString(),
                    address);
        }
	} catch (UnknownHostException e) {
		return null;
	}
}

@Override
void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeByteArray(address);
}

}
