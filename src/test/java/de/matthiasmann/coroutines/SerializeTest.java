/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.coroutines;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

/** @author Matthias Mann */
public class SerializeTest {
  @Test
  public void testSerialize()
      throws IOException, ClassNotFoundException {
    Iterator<String> iter1 = new TestIterator();

    assertEquals("A", iter1.next());
    assertEquals("B", iter1.next());
    assertEquals("C0", iter1.next());
    assertEquals("C1", iter1.next());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);

    oos.writeObject(iter1);
    oos.close();

    byte[] bytes = baos.toByteArray();

    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    ObjectInputStream ois = new ObjectInputStream(bais);
    @SuppressWarnings("unchecked")
    Iterator<String> iter2 = (Iterator<String>) ois.readObject();

    assertNotSame(iter1, iter2);

    assertEquals("C2", iter2.next());
    assertEquals("C3", iter2.next());
    assertEquals("D", iter2.next());
    assertEquals("E", iter2.next());
    assertFalse(iter2.hasNext());

    assertEquals("C2", iter1.next());
    assertEquals("C3", iter1.next());
    assertEquals("D", iter1.next());
    assertEquals("E", iter1.next());
    assertFalse(iter1.hasNext());
  }

  private static class TestIterator extends CoIterator<String>
    implements Serializable {
    /**  */
    private static final long serialVersionUID = -6350955430126473176L;

    /**
     * DOCUMENT ME!
     *
     * @throws SuspendExecution DOCUMENT ME!
     */
    @Override
    protected void run()
        throws SuspendExecution {
      produce("A");
      produce("B");

      for (int i = 0; i < 4; i++) {
        produce("C" + i);
      }

      produce("D");
      produce("E");
    }
  }
}
