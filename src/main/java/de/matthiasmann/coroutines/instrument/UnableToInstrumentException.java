/*
 * Copyright (c) 2008, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.coroutines.instrument;

/**
 * <p>This exception is thrown when an unsupported construct was found in a
 * class that must be instrumented for suspension.</p>
 *
 * <p>Note: this needs to be a RuntimeException - otherwise it can't be thrown
 * from {@link CheckInstrumentationVisitor}.</p>
 *
 * @author Matthias Mann
 */
public class UnableToInstrumentException extends RuntimeException {
  /**  */
  private static final long serialVersionUID = 7628714314153907666L;
  private final String reason;
  private final String className;
  private final String methodName;

  /**
   * Creates a new UnableToInstrumentException object.
   *
   * @param reason     DOCUMENT ME!
   * @param className  DOCUMENT ME!
   * @param methodName DOCUMENT ME!
   */
  public UnableToInstrumentException(
    String reason, String className, String methodName
  ) {
    super(
      String.format(
        "Unable to instrument class %s#%s because of %s", className,
        methodName, reason
      )
    );
    this.reason = reason;
    this.className = className;
    this.methodName = methodName;
  }

  /**
   * Returns the value of the class name property.
   *
   * @return the value of the class name property.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the value of the method name property.
   *
   * @return the value of the method name property.
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Returns the value of the reason property.
   *
   * @return the value of the reason property.
   */
  public String getReason() {
    return reason;
  }

}
