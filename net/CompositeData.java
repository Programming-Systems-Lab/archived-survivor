package wfruntime.psl;

import java.io.Serializable;

/**
 * @author Gaurav S. Kc [gskc@cs.columbia.edu]
 * @author Jean-Denis Greze [jg253@cs.columbia.edu]
 *
 * Copyright (c) 2001
 * The Trustees of Columbia University and the City of New York
 * All Rights Reserved.
*/

interface CompositeData extends Serializable {
  Object getKey();
  Object getData();
}
