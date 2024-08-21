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

public class DataFlowException extends ShamanException implements Serializable
{
    public final static long serialVersionUID = 0L;
    
    public DataFlowException(String message) {
        super(message);
    }
    
    public DataFlowException(Throwable throwable) {
        super(throwable);
    }

    public DataFlowException(String message, Throwable throwable) {
        super(message,throwable);
    }
}
