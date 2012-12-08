/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.coroutines;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** @author mam */
public class InheritTest {
  @Test
  public void testInherit() {
    final C dut = new C();
    Coroutine c =
      new Coroutine(
        new CoRunnable() {
          public void coExecute()
              throws SuspendExecution {
            dut.myMethod();
          }
        }
      );

    for (int i = 0; i < 3; i++) {
      c.run();
    }

    assertEquals(5, dut.result.size());
    assertEquals("a", dut.result.get(0));
    assertEquals("o1", dut.result.get(1));
    assertEquals("o2", dut.result.get(2));
    assertEquals("b", dut.result.get(3));
    assertEquals("b", dut.result.get(4));
  }

  /**
   * DOCUMENT ME!
   *
   * @author  $author$
   * @version $Revision$, $Date$
   */
  public static class A {
    /**
     * DOCUMENT ME!
     *
     * @throws SuspendExecution DOCUMENT ME!
     */
    public static void yield()
        throws SuspendExecution {
      Coroutine.yield();
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @author  $author$
   * @version $Revision$, $Date$
   */
  public static class B extends A {
    final ArrayList<String> result = new ArrayList<String>();
  }

  /**
   * DOCUMENT ME!
   *
   * @author  $author$
   * @version $Revision$, $Date$
   */
  public static class C extends B {
    /**
     * DOCUMENT ME!
     *
     * @throws SuspendExecution DOCUMENT ME!
     */
    public void otherMethod()
        throws SuspendExecution {
      result.add("o1");
      Coroutine.yield();
      result.add("o2");
    }

    /**
     * DOCUMENT ME!
     *
     * @throws SuspendExecution DOCUMENT ME!
     */
    public void myMethod()
        throws SuspendExecution {
      result.add("a");
      otherMethod();

      for (;;) {
        result.add("b");

        if (result.size() > 10) {
          otherMethod();
          result.add("Ohh!");
        }

        yield();
      }
    }
  }
}
