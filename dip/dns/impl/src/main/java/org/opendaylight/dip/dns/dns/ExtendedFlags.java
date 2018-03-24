/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.dns;

/**
 * Constants and functions relating to EDNS flags.
 *
 * @author Brian Wellington
 */

public final class ExtendedFlags {

private static Mnemonic extflags = new Mnemonic("EDNS Flag",
						Mnemonic.CASE_LOWER);

/** dnssec ok */
public static final int DO		= 0x8000;

static {
	extflags.setMaximum(0xFFFF);
	extflags.setPrefix("FLAG");
	extflags.setNumericAllowed(true);

	extflags.add(DO, "do");
}

private ExtendedFlags() {}

/** Converts a numeric extended flag into a String */
public static String string(int i) {
	return extflags.getText(i);
}

/**
 * Converts a textual representation of an extended flag into its numeric
 * value
 */
public static int value(String s) {
	return extflags.getValue(s);
}

}
