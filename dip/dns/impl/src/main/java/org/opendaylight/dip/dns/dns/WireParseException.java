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
 * An exception thrown when a dns message is invalid.
 *
 * @author Brian Wellington
 */

public class WireParseException extends IOException {

public
WireParseException() {
	super();
}

public WireParseException(String s) {
	super(s);
}

public
WireParseException(String s, Throwable cause) {
	super(s);
	initCause(cause);
}

}
