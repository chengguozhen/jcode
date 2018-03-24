/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

/**
 * The Name Server Identifier Option, define in RFC 5001.
 *
 * @see OPTRecord
 * 
 * @author Brian Wellington
 */
public class NSIDOption extends GenericEDNSOption {

private static final long serialVersionUID = 74739759292589056L;

NSIDOption() {
	super(EDNSOption.Code.NSID);
}

/**
 * Construct an NSID option.
 * @param data The contents of the option.
 */
public NSIDOption(byte [] data) {
	super(EDNSOption.Code.NSID, data);
}

}
