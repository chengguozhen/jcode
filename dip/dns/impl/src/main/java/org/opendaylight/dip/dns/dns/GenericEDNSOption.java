/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;
import org.opendaylight.dip.dns.dns.utils.base16;
import java.io.*;


/**
 * An EDNSOption with no internal structure.
 * 
 * @author Ming Zhou &lt;mizhou@bnivideo.com&gt;, Beaumaris Networks
 * @author Brian Wellington
 */
public class GenericEDNSOption extends EDNSOption {

private byte [] data;

GenericEDNSOption(int code) {
	super(code);
}

/**
 * Construct a generic EDNS option.
 * @param data The contents of the option.
 */
public 
GenericEDNSOption(int code, byte [] data) {
	super(code);
	this.data = Record.checkByteArrayLength("option data", data, 0xFFFF);
}

@Override
void
optionFromWire(DNSInput in) throws IOException {
	data = in.readByteArray();
}

@Override
void
optionToWire(DNSOutput out) {
	out.writeByteArray(data);
}

@Override
String
optionToString() {
	return "<" + base16.toString(data) + ">";
}

}
