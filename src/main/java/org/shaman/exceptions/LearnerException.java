/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.exceptions;

import java.io.Serializable;

public class LearnerException extends ShamanException implements Serializable
{
    public final static long serialVersionUID = 0L;
    
    public LearnerException(String message) {
        super(message);
    }
    
    public LearnerException(Throwable throwable) {
        super(throwable);
    }

    public LearnerException(String message, Throwable throwable) {
        super(message,throwable);
    }
}
