/*
 * Joda Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-2004 Stephen Colebourne.  
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Joda project (http://www.joda.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The name "Joda" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact licence@joda.org.
 *
 * 5. Products derived from this software may not be called "Joda",
 *    nor may "Joda" appear in their name, without prior written
 *    permission of the Joda project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE JODA AUTHORS OR THE PROJECT
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Joda project and was originally 
 * created by Stephen Colebourne <scolebourne@joda.org>. For more
 * information on the Joda project, please see <http://www.joda.org/>.
 */
package org.joda.time.base;

import java.io.Serializable;

import org.joda.time.DateTimeUtils;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.joda.time.convert.ConverterManager;
import org.joda.time.convert.DurationConverter;
import org.joda.time.field.FieldUtils;

/**
 * BaseDuration is an abstract implementation of ReadableDuration that stores
 * data in a <code>long</code> duration milliseconds field.
 * <p>
 * This class should generally not be used directly by API users.
 * The {@link ReadableDuration} interface should be used when different 
 * kinds of duration objects are to be referenced.
 * <p>
 * BaseDuration subclasses may be mutable and not thread-safe.
 *
 * @author Brian S O'Neill
 * @author Stephen Colebourne
 * @since 1.0
 */
public class BaseDuration
        extends AbstractDuration
        implements ReadableDuration, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 2581698638990L;

    /** The duration length */
    private long iMillis;

    /**
     * Creates a duration from the given millisecond duration.
     *
     * @param duration  the duration, in milliseconds
     */
    public BaseDuration(long duration) {
        super();
        iMillis = duration;
    }

    /**
     * Creates a duration from the given interval endpoints.
     *
     * @param startInstant  interval start, in milliseconds
     * @param endInstant  interval end, in milliseconds
     * @throws ArithmeticException if the duration exceeds a 64 bit long
     */
    public BaseDuration(long startInstant, long endInstant) {
        super();
        iMillis = FieldUtils.safeAdd(endInstant, -startInstant);
    }

    /**
     * Creates a duration from the given interval endpoints.
     *
     * @param start  interval start, null means now
     * @param end  interval end, null means now
     * @throws ArithmeticException if the duration exceeds a 64 bit long
     */
    public BaseDuration(ReadableInstant start, ReadableInstant end) {
        super();
        if (start == end) {
            iMillis = 0L;
        } else {
            long startMillis = DateTimeUtils.getInstantMillis(start);
            long endMillis = DateTimeUtils.getInstantMillis(end);
            iMillis = FieldUtils.safeAdd(endMillis, -startMillis);
        }
    }

    /**
     * Creates a duration from the specified object using the
     * {@link org.joda.time.convert.ConverterManager ConverterManager}.
     *
     * @param duration  duration to convert
     * @throws IllegalArgumentException if duration is invalid
     */
    public BaseDuration(Object duration) {
        super();
        DurationConverter converter = ConverterManager.getInstance().getDurationConverter(duration);
        iMillis = converter.getDurationMillis(duration);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the length of this duration in milliseconds.
     *
     * @return the length of the duration in milliseconds.
     */
    public long getMillis() {
        return iMillis;
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the length of this duration in milliseconds.
     * 
     * @param duration  the new length of the duration
     */
    protected void setMillis(long duration) {
        iMillis = duration;
    }

}
