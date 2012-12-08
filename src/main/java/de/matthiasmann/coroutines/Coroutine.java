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

import java.io.IOException;
import java.io.Serializable;

import de.matthiasmann.coroutines.instrument.AlreadyInstrumented;

/**
 * <p>A Coroutine is used to run a CoroutineProto.</p>
 *
 * <p>It also provides a function to suspend a running Coroutine.</p>
 *
 * <p>A Coroutine can be serialized if it's not running and all involved
 * classes and data types are also {@link Serializable}.</p>
 *
 * @author Matthias Mann
 */
public class Coroutine implements Runnable, Serializable {
  private static final long serialVersionUID = 2783452871536981L;

  /**
   * DOCUMENT ME!
   *
   * @author  $author$
   * @version $Revision$, $Date$
   */
  public enum State {
    /** The Coroutine has not yet been executed. */
    NEW,

    /** The Coroutine is currently executing. */
    RUNNING,

    /** The Coroutine has suspended it's execution. */
    SUSPENDED,

    /**
     * The Coroutine has finished it's run method.
     *
     * @see CoroutineProto#coExecute()
     */
    FINISHED
  }

  private final CoRunnable proto;
  private final Stack stack;
  private State state;

  /**
   * Creates a new Coroutine from the given CoroutineProto. A CoroutineProto
   * can be used in several Coroutines at the same time - but then the normal
   * multi threading rules apply to the member state.
   *
   * @param  proto The CoroutineProto for the Coroutine.
   *
   * @throws NullPointerException DOCUMENT ME!
   */
  public Coroutine(CoRunnable proto) {
    this.proto = proto;
    this.stack = new Stack(this);
    this.state = State.NEW;

    if (proto == null) {
      throw new NullPointerException("proto");
    }

    assert isInstrumented(proto): "Not instrumented";
  }

  /**
   * Suspend the currently running Coroutine on the calling thread.
   *
   * @throws SuspendExecution      de.matthiasmann.coroutines.SuspendExecution
   *                               This exception is used for controltransfer
   *                               - don't catch it !
   * @throws IllegalStateException If not called from a Coroutine
   * @throws Error                 DOCUMENT ME!
   */
  public static void yield()
      throws SuspendExecution, IllegalStateException {
    throw new Error("Calling function not instrumented");
  }

  /**
   * Returns the active Coroutine on this thread or NULL if no coroutine is
   * running.
   *
   * @return the active Coroutine on this thread or NULL if no coroutine is
   *         running.
   */
  public static Coroutine getActiveCoroutine() {
    Stack s = Stack.getStack();

    if (s != null) {
      return s.co;
    }

    return null;
  }

  /**
   * Returns the CoroutineProto that is used for this Coroutine.
   *
   * @return The CoroutineProto that is used for this Coroutine
   */
  public CoRunnable getProto() {
    return proto;
  }

  /**
   * <p>Returns the current state of this Coroutine. May be called by the
   * Coroutine itself but should not be called by another thread.</p>
   *
   * <p>The Coroutine starts in the state NEW then changes to RUNNING. From
   * RUNNING it may change to FINISHED or SUSPENDED. SUSPENDED can only change
   * to RUNNING by calling run() again.</p>
   *
   * @return The current state of this Coroutine
   *
   * @see    #run()
   */
  public State getState() {
    return state;
  }

  /**
   * Runs the Coroutine until it is finished or suspended. This method must
   * only be called when the Coroutine is in the states NEW or SUSPENDED. It
   * is not multi threading safe.
   *
   * @throws IllegalStateException if the Coroutine is currently running or
   *                               already finished.
   */
  public void run()
      throws IllegalStateException {
    if ((state != State.NEW) && (state != State.SUSPENDED)) {
      throw new IllegalStateException("Not new or suspended");
    }

    State result = State.FINISHED;
    Stack oldStack = Stack.getStack();

    try {
      state = State.RUNNING;
      Stack.setStack(stack);

      try {
        proto.coExecute();
      } catch (SuspendExecution ex) {
        assert ex == SuspendExecution.instance;
        result = State.SUSPENDED;

        // stack.dump();
        stack.resumeStack();
      }
    } finally {
      Stack.setStack(oldStack);
      state = result;
    }
  }

  private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    if (state == State.RUNNING) {
      throw new IllegalStateException(
        "trying to serialize a running coroutine"
      );
    }

    out.defaultWriteObject();
  }

  private boolean isInstrumented(CoRunnable proto) {
    try {
      return proto.getClass().isAnnotationPresent(AlreadyInstrumented.class);
    } catch (Throwable ex) {
      return
        true; // it's just a check - make sure we don't fail if something goes
              // wrong
    }
  }
}
