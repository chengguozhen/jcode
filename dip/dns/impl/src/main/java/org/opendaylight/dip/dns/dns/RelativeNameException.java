/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

/**
 * An exception thrown when a relative name is passed as an argument to
 * a method requiring an absolute name.
 *
 * @author Brian Wellington
 */

public class RelativeNameException extends IllegalArgumentException {

public
RelativeNameException(Name name) {
	super("'" + name + "' is not an absolute name");
}

public
RelativeNameException(String s) {
	super(s);
}

}
