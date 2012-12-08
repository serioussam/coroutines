/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.matthiasmann.coroutines;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

/** @author mam */
public class MergeTest implements CoRunnable {
  /**
   * DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public static void throwsIO()
      throws IOException {}

  /**
   * DOCUMENT ME!
   *
   * @throws SuspendExecution DOCUMENT ME!
   */
  public void coExecute()
      throws SuspendExecution {
    try {
      throwsIO();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testMerge() {
    Coroutine c = new Coroutine(new MergeTest());

    c.run();
  }
}
