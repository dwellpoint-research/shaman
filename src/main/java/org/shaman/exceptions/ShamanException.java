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

import org.apache.commons.lang.exception.NestableException;

public class ShamanException extends NestableException implements Serializable
{
    public final static long serialVersionUID = 0L;
    
    public ShamanException(String message) {
        super(message);
    }
    
    public ShamanException(Throwable throwable) {
        super(throwable);
    }

    public ShamanException(String message, Throwable throwable) {
        super(message,throwable);
    }
}
