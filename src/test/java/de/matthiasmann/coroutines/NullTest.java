/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.coroutines;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** @author Matthias Mann */
public class NullTest implements CoRunnable {
  Object result = "b";

  @Test
  public void testNull() {
    int count = 0;
    Coroutine co = new Coroutine(this);

    while (co.getState() != Coroutine.State.FINISHED) {
      ++count;
      co.run();
    }

    assertEquals(2, count);
    assertEquals("a", result);
  }

  /**
   * DOCUMENT ME!
   *
   * @throws SuspendExecution DOCUMENT ME!
   */
  public void coExecute()
      throws SuspendExecution {
    result = getProperty();
  }

  private Object getProperty()
      throws SuspendExecution {
    Object x = null;

    Object y = getProtery("a");

    if (y != null) {
      x = y;
    }

    return x;
  }

  private Object getProtery(String string)
      throws SuspendExecution {
    Coroutine.yield();

    return string;
  }

}
