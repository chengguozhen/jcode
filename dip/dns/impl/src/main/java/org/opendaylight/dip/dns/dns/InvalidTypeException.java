/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

/**
 * An exception thrown when an invalid type code is specified.
 *
 * @author Brian Wellington
 */

public class InvalidTypeException extends IllegalArgumentException {

public
InvalidTypeException(int type) {
	super("Invalid dns type: " + type);
}

}
