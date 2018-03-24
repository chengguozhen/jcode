/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

import java.io.IOException;

/**
 * A class implementing Records of unknown and/or unimplemented types.  This
 * class can only be initialized using static Record initializers.
 *
 * @author Brian Wellington
 */

public class UNKRecord extends Record {

private static final long serialVersionUID = -4193583311594626915L;

private byte [] data;

UNKRecord() {}

@Override
Record
getObject() {
	return new UNKRecord();
}

@Override
void
rrFromWire(DNSInput in) throws IOException {
	data = in.readByteArray();
}

@Override
void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	throw st.exception("invalid unknown RR encoding");
}

/** Converts this Record to the String "unknown format" */
@Override
String
rrToString() {
	return unknownToString(data);
}

/** Returns the contents of this record. */
public byte []
getData() { 
	return data;
}

@Override
void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeByteArray(data);
}

}
