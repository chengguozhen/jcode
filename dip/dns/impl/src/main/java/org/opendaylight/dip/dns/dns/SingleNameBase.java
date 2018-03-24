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
 * Implements common functionality for the many record types whose format
 * is a single name.
 *
 * @author Brian Wellington
 */

abstract class SingleNameBase extends Record {

private static final long serialVersionUID = -18595042501413L;

protected Name singleName;

protected
SingleNameBase() {}

protected
SingleNameBase(Name name, int type, int dclass, long ttl) {
	super(name, type, dclass, ttl);
}

protected
SingleNameBase(Name name, int type, int dclass, long ttl, Name singleName,
	    String description)
{
	super(name, type, dclass, ttl);
	this.singleName = checkName(description, singleName);
}

@Override
void
rrFromWire(DNSInput in) throws IOException {
	singleName = new Name(in);
}

@Override
void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	singleName = st.getName(origin);
}

@Override
String
rrToString() {
	return singleName.toString();
}

protected Name
getSingleName() {
	return singleName;
}

@Override
void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	singleName.toWire(out, null, canonical);
}

}
