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

public class TransformationException extends DataFlowException implements Serializable
{
    public final static long serialVersionUID = 0L;
    
    public TransformationException(String message) {
        super(message);
    }
    
    public TransformationException(Throwable throwable) {
        super(throwable);
    }

    public TransformationException(String message, Throwable throwable) {
        super(message,throwable);
    }
}
