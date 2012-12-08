/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.coroutines;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** @author Matthias Mann */
public class ArrayTest implements CoRunnable {
  private static final PatchLevel l1 = new PatchLevel();
  private static final PatchLevel[] l2 = new PatchLevel[] { l1 };
  private static final PatchLevel[][] l3 = new PatchLevel[][] { l2 };

  /** DOCUMENT ME! */
  @Test
  public void testArray() {
    Coroutine co = new Coroutine(this);

    co.run();
    assertEquals(42, l1.i);
  }

  /**
   * DOCUMENT ME!
   *
   * @throws SuspendExecution DOCUMENT ME!
   */
  public void coExecute()
      throws SuspendExecution {
    PatchLevel[][] local_patch_levels = l3;
    PatchLevel patch_level = local_patch_levels[0][0];

    patch_level.setLevel(42);
  }

  /**
   * DOCUMENT ME!
   *
   * @author  $author$
   * @version $Revision$, $Date$
   */
  public static class PatchLevel {
    int i;

    /**
     * Sets the value of the level property.
     *
     * @param  value the new value of the level property.
     *
     * @throws SuspendExecution DOCUMENT ME!
     */
    public void setLevel(int value)
        throws SuspendExecution {
      i = value;
    }
  }
}
