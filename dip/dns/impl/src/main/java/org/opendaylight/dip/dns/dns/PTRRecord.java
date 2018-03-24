/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

/**
 * Pointer Record  - maps a domain name representing an Internet Address to
 * a hostname.
 *
 * @author Brian Wellington
 */

public class PTRRecord extends SingleCompressedNameBase {

private static final long serialVersionUID = -8321636610425434192L;

PTRRecord() {}

@Override
Record getObject() {
	return new PTRRecord();
}

/** 
 * Creates a new PTR Record with the given data
 * @param target The name of the machine with this address
 */
public PTRRecord(Name name, int dclass, long ttl, Name target) {
	super(name, Type.PTR, dclass, ttl, target, "target");
}

/** Gets the target of the PTR Record */
public Name getTarget() {
	return getSingleName();
}

}
