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
package de.matthiasmann.coroutines;

import java.io.Serializable;

/**
 * Internal Class - DO NOT USE !
 *
 * <p>Needs to be public so that instrumented code can access it. ANY CHANGE
 * IN THIS CLASS NEEDS TO BE SYNCHRONIZED WITH {@link
 * de.matthiasmann.coroutines.instrument.InstrumentMethod}</p>
 *
 * @author Matthias Mann
 */
public final class Stack implements Serializable {
  private static final long serialVersionUID = 12786283751253L;

  private static final ThreadLocal<Stack> tls = new ThreadLocal<Stack>();

  /** sadly this need to be here. */
  public static SuspendExecution exception_instance_not_for_user_code =
    SuspendExecution.instance;

  final Coroutine co;

  private int methodTOS = -1;
  private int[] method = new int[16];

  private long[] dataLong = new long[32];
  private Object[] dataObject = new Object[32];

  transient int curMethodSP;

  Stack(Coroutine co) {
    this.co = co;
  }

  /**
   * Returns the value of the stack property.
   *
   * @return the value of the stack property.
   */
  public static Stack getStack() {
    return tls.get();
  }

  static void setStack(Stack s) {
    tls.set(s);
  }

  /**
   * Called before a method is called.
   *
   * @param entry    the entry point in the method for resume
   * @param numSlots the number of required stack slots for storing the state
   */
  public void pushMethodAndReserveSpace(int entry, int numSlots) {
    final int methodIdx = methodTOS;

    if ((method.length - methodIdx) < 2) {
      growMethodStack();
    }

    curMethodSP = method[methodIdx - 1];

    int dataTOS = curMethodSP + numSlots;

    method[methodIdx] = entry;
    method[methodIdx + 1] = dataTOS;

    // System.out.println("entry="+entry+" size="+size+" sp="+curMethodSP+"
    // tos="+dataTOS+" nr="+methodIdx);

    if (dataTOS > dataObject.length) {
      growDataStack(dataTOS);
    }
  }

  /**
   * Called at the end of a method - undoes the effects of nextMethodEntry().
   *
   * @param numObjSlots number of Object slots to clear
   */
  public void popMethod(int numObjSlots) {
    int idx = methodTOS;

    method[idx] = 0;
    curMethodSP = method[idx - 1];
    methodTOS = idx - 2;
    clearStack(numObjSlots);
  }

  /**
   * called at the begin of a method.
   *
   * @return the entry point of this method
   */
  public int nextMethodEntry() {
    int idx = methodTOS;

    curMethodSP = method[++idx];
    methodTOS = ++idx;

    return method[idx];
  }

  /**
   * DOCUMENT ME!
   *
   * @param value DOCUMENT ME!
   * @param s     DOCUMENT ME!
   * @param idx   DOCUMENT ME!
   */
  public static void push(int value, Stack s, int idx) {
    s.dataLong[s.curMethodSP + idx] = value;
  }

  /**
   * DOCUMENT ME!
   *
   * @param value DOCUMENT ME!
   * @param s     DOCUMENT ME!
   * @param idx   DOCUMENT ME!
   */
  public static void push(float value, Stack s, int idx) {
    s.dataLong[s.curMethodSP + idx] = Float.floatToRawIntBits(value);
  }

  /**
   * DOCUMENT ME!
   *
   * @param value DOCUMENT ME!
   * @param s     DOCUMENT ME!
   * @param idx   DOCUMENT ME!
   */
  public static void push(long value, Stack s, int idx) {
    s.dataLong[s.curMethodSP + idx] = value;
  }

  /**
   * DOCUMENT ME!
   *
   * @param value DOCUMENT ME!
   * @param s     DOCUMENT ME!
   * @param idx   DOCUMENT ME!
   */
  public static void push(double value, Stack s, int idx) {
    s.dataLong[s.curMethodSP + idx] = Double.doubleToRawLongBits(value);
  }

  /**
   * DOCUMENT ME!
   *
   * @param value DOCUMENT ME!
   * @param s     DOCUMENT ME!
   * @param idx   DOCUMENT ME!
   */
  public static void push(Object value, Stack s, int idx) {
    s.dataObject[s.curMethodSP + idx] = value;
  }

  /**
   * Returns the value of the int property.
   *
   * @param  idx DOCUMENT ME!
   *
   * @return the value of the int property.
   */
  public int getInt(int idx) {
    return (int) dataLong[curMethodSP + idx];
  }

  /**
   * Returns the value of the float property.
   *
   * @param  idx DOCUMENT ME!
   *
   * @return the value of the float property.
   */
  public float getFloat(int idx) {
    return Float.intBitsToFloat((int) dataLong[curMethodSP + idx]);
  }

  /**
   * Returns the value of the long property.
   *
   * @param  idx DOCUMENT ME!
   *
   * @return the value of the long property.
   */
  public long getLong(int idx) {
    return dataLong[curMethodSP + idx];
  }

  /**
   * Returns the value of the double property.
   *
   * @param  idx DOCUMENT ME!
   *
   * @return the value of the double property.
   */
  public double getDouble(int idx) {
    return Double.longBitsToDouble(dataLong[curMethodSP + idx]);
  }

  /**
   * Returns the value of the object property.
   *
   * @param  idx DOCUMENT ME!
   *
   * @return the value of the object property.
   */
  public Object getObject(int idx) {
    return dataObject[curMethodSP + idx];
  }

  /** called when resuming a stack. */
  void resumeStack() {
    methodTOS = -1;
  }

  /* DEBUGGING CODE
   * public void dump() { int sp = 0; for(int i=0 ; i<=methodTOS ; i++) {
   * System.out.println("i="+i+" entry="+methodEntry[i]+" sp="+methodSP[i]);
   * for(; sp < methodSP[i+1] ; sp++) { System.out.println("sp="+sp+"
   * long="+dataLong[sp]+" obj="+dataObject[sp]);     } } }
   */

  private void clearStack(int cnt) {
    for (int i = 0; i < cnt; i++) {
      dataObject[curMethodSP + i] = null;
    }
  }

  private void growDataStack(int required) {
    int newSize = dataObject.length;

    do {
      newSize *= 2;
    } while (newSize < required);

    dataLong = Util.copyOf(dataLong, newSize);
    dataObject = Util.copyOf(dataObject, newSize);
  }

  private void growMethodStack() {
    int newSize = methodTOS * 2;

    method = Util.copyOf(method, newSize);
  }
}
