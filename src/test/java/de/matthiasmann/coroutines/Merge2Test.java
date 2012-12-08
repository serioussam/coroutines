/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.matthiasmann.coroutines;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


/** @author mam */
public class Merge2Test implements CoRunnable {
  /**
   * Returns the value of the interface property.
   *
   * @return the value of the interface property.
   */
  public static Interface getInterface() {
    return null;
  }

  /**
   * DOCUMENT ME!
   *
   * @throws SuspendExecution DOCUMENT ME!
   */
  public static void suspendable()
      throws SuspendExecution {}

  /**
   * DOCUMENT ME!
   *
   * @throws SuspendExecution DOCUMENT ME!
   */
  public void coExecute()
      throws SuspendExecution {
    try {
      Interface iface = getInterface();

      iface.method();
    } catch (IllegalStateException ise) {
      suspendable();
    }
  }

  /** DOCUMENT ME! */
  @Test
  public void testMerge2() {
    try {
      Coroutine c = new Coroutine(new Merge2Test());

      c.run();
      assertTrue("Should not reach here", false);
    } catch (NullPointerException ex) {
      // NPE expected
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @author  $author$
   * @version $Revision$, $Date$
   */
  public interface Interface {
    /** DOCUMENT ME! */
    void method();
  }
}
