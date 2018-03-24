/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.dns;

/**
 * DNAME Record  - maps a nonterminal alias (subtree) to a different domain
 *
 * @author Brian Wellington
 */

public class DNAMERecord extends SingleNameBase {

private static final long serialVersionUID = 2670767677200844154L;

DNAMERecord() {}

@Override
Record
getObject() {
	return new DNAMERecord();
}

/**
 * Creates a new DNAMERecord with the given data
 * @param alias The name to which the DNAME alias points
 */
public DNAMERecord(Name name, int dclass, long ttl, Name alias) {
	super(name, Type.DNAME, dclass, ttl, alias, "alias");
}

/**
 * Gets the target of the DNAME Record
 */
public Name
getTarget() {
	return getSingleName();
}

/** Gets the alias specified by the DNAME Record */
public Name
getAlias() {
	return getSingleName();
}

}
