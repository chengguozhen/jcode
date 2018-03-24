/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

import java.io.*;

/**
 * A class implementing Records with no data; that is, records used in
 * the question section of messages and meta-records in dynamic update.
 *
 * @author Brian Wellington
 */

class EmptyRecord extends Record {

private static final long serialVersionUID = 3601852050646429582L;

EmptyRecord() {}

@Override
Record getObject() {
	return new EmptyRecord();
}

@Override
void rrFromWire(DNSInput in) throws IOException {
}

@Override
void rdataFromString(Tokenizer st, Name origin) throws IOException {
}

@Override
String rrToString() {
	return "";
}

@Override
void rrToWire(DNSOutput out, Compression c, boolean canonical) {
}

}
