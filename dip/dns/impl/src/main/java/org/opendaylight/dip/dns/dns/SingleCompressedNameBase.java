/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

/**
 * Implements common functionality for the many record types whose format
 * is a single compressed name.
 *
 * @author Brian Wellington
 */

abstract class SingleCompressedNameBase extends SingleNameBase {

private static final long serialVersionUID = -236435396815460677L;

protected
SingleCompressedNameBase() {}

protected
SingleCompressedNameBase(Name name, int type, int dclass, long ttl,
			 Name singleName, String description)
{
        super(name, type, dclass, ttl, singleName, description);
}

@Override
void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	singleName.toWire(out, c, canonical);
}

}
