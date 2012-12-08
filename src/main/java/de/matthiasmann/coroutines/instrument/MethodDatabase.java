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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;

/**
 * <p>Collects information about classes and their suspendable methods.</p>
 *
 * <p>Provides access to configuration parameters and to logging</p>
 *
 * @author Matthias Mann
 */
public class MethodDatabase implements Log {
  private final ClassLoader cl;
  private final HashMap<String, ClassEntry> classes;
  private final HashMap<String, String> superClasses;
  private final ArrayList<File> workList;

  private Log log;
  private boolean verbose;
  private boolean debug;
  private boolean allowMonitors;

  /**
   * Creates a new MethodDatabase object.
   *
   * @param cl DOCUMENT ME!
   */
  public MethodDatabase(ClassLoader cl) {
    this.cl = cl;

    classes = new HashMap<String, ClassEntry>();
    superClasses = new HashMap<String, String>();
    workList = new ArrayList<File>();
  }

  /**
   * Returns the value of the allow monitors property.
   *
   * @return the value of the allow monitors property.
   */
  public boolean isAllowMonitors() {
    return allowMonitors;
  }

  /**
   * Sets the value of the allow monitors property.
   *
   * @param allowMonitors the new value of the allow monitors property.
   */
  public void setAllowMonitors(boolean allowMonitors) {
    this.allowMonitors = allowMonitors;
  }

  /**
   * Returns the value of the log property.
   *
   * @return the value of the log property.
   */
  public Log getLog() {
    return log;
  }

  /**
   * Sets the value of the log property.
   *
   * @param log the new value of the log property.
   */
  public void setLog(Log log) {
    this.log = log;
  }

  /**
   * Returns the value of the verbose property.
   *
   * @return the value of the verbose property.
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets the value of the verbose property.
   *
   * @param verbose the new value of the verbose property.
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Returns the value of the debug property.
   *
   * @return the value of the debug property.
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * Sets the value of the debug property.
   *
   * @param debug the new value of the debug property.
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * DOCUMENT ME!
   *
   * @param msg  DOCUMENT ME!
   * @param args DOCUMENT ME!
   */
  public void log(String msg, Object ...args) {
    if (log != null) {
      log.log(msg, args);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param msg DOCUMENT ME!
   * @param ex  DOCUMENT ME!
   */
  public void error(String msg, Exception ex) {
    if (log != null) {
      log.error(msg, ex);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param f DOCUMENT ME!
   */
  public void checkClass(File f) {
    try {
      FileInputStream fis = new FileInputStream(f);
      CheckInstrumentationVisitor civ = checkFileAndClose(fis, f.getPath());

      if ((civ != null) && civ.needsInstrumentation()) {
        if (civ.isAlreadyInstrumented()) {
          log("Found instrumented class: %s", f.getPath());
        } else {
          log("Found class: %s", f.getPath());
        }

        classes.put(civ.getName(), new ClassEntry(civ));

        if ((f != null) && !civ.isAlreadyInstrumented()) {
          workList.add(f);
        }
      }
    } catch (UnableToInstrumentException ex) {
      throw ex;
    } catch (Exception ex) {
      error(f.getPath(), ex);
    }
  }

  /**
   * Returns the value of the method suspendable property.
   *
   * @param  className  DOCUMENT ME!
   * @param  methodName DOCUMENT ME!
   *
   * @return the value of the method suspendable property.
   */
  public boolean isMethodSuspendable(String className, String methodName) {
    if (methodName.charAt(0) == '<') {
      return false; // special methods are never suspendable
    }

    for (;;) {
      if (isJavaCore(className)) {
        return false;
      }

      ClassEntry entry = classes.get(className);

      if ((entry == null) && (cl != null)) {
        log("trying to read class: %s", className);

        CheckInstrumentationVisitor civ = checkClass(className);

        if (civ == null) {
          log("Class not found assuming suspendable: %s", className);

          return true;
        }

        entry = new ClassEntry(civ);
        classes.put(className, entry);
      }

      if (entry == null) {
        log("Can't check class - assuming suspendable: %s", className);

        return true;
      }

      for (String method : entry.methods) {
        if (method.equals(methodName)) {
          return true;
        }
      }

      className = entry.parent;
    }
  }

  /**
   * Returns the value of the common super class property.
   *
   * @param  classA DOCUMENT ME!
   * @param  classB DOCUMENT ME!
   *
   * @return the value of the common super class property.
   */
  public String getCommonSuperClass(String classA, String classB) {
    ArrayList<String> listA = getSuperClasses(classA);
    ArrayList<String> listB = getSuperClasses(classB);

    if ((listA == null) || (listB == null)) {
      return null;
    }

    int idx = 0;
    int num = Math.min(listA.size(), listB.size());

    for (; idx < num; idx++) {
      String superClassA = listA.get(idx);
      String superClassB = listB.get(idx);

      if (!superClassA.equals(superClassB)) {
        break;
      }
    }

    if (idx > 0) {
      return listA.get(idx - 1);
    }

    return null;
  }

  /**
   * Returns the value of the exception property.
   *
   * @param  className DOCUMENT ME!
   *
   * @return the value of the exception property.
   */
  public boolean isException(String className) {
    for (;;) {
      if ("java/lang/Throwable".equals(className)) {
        return true;
      }

      if ("java/lang/Object".equals(className)) {
        return false;
      }

      String superClass = getDirectSuperClass(className);

      if (superClass == null) {
        log("Can't determine super class of %s", className);

        return false;
      }

      className = superClass;
    }
  }

  /**
   * Returns the value of the work list property.
   *
   * @return the value of the work list property.
   */
  public ArrayList<File> getWorkList() {
    return workList;
  }

  /**
   * <p>Overwrite this function if Coroutines is used in a transformation
   * chain.</p>
   *
   * <p>This method must create a new CheckInstrumentationVisitor and visit
   * the specified class with it.</p>
   *
   * @param  className the class the needs to be analysed
   *
   * @return a new CheckInstrumentationVisitor that has visited the specified
   *         class or null if the class was not found
   */
  protected CheckInstrumentationVisitor checkClass(String className) {
    InputStream is = cl.getResourceAsStream(className + ".class");

    if (is != null) {
      return checkFileAndClose(is, className);
    }

    return null;
  }

  private CheckInstrumentationVisitor checkFileAndClose(
    InputStream is, String name
  ) {
    try {
      try {
        ClassReader r = new ClassReader(is);

        CheckInstrumentationVisitor civ = new CheckInstrumentationVisitor();

        r.accept(civ, 0);

        return civ;
      } finally {
        is.close();
      }
    } catch (UnableToInstrumentException ex) {
      throw ex;
    } catch (Exception ex) {
      error(name, ex);
    }

    return null;
  }

  private String extractSuperClass(String className) {
    InputStream is = cl.getResourceAsStream(className + ".class");

    if (is != null) {
      try {
        try {
          ClassReader r = new ClassReader(is);
          ExtractSuperClass esc = new ExtractSuperClass();

          r.accept(
            esc,
            ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
            | ClassReader.SKIP_FRAMES
          );

          return esc.superClass;
        } finally {
          is.close();
        }
      } catch (IOException ex) {
        error(className, ex);
      }
    }

    return null;
  }

  private ArrayList<String> getSuperClasses(String className) {
    ArrayList<String> result = new ArrayList<String>();

    for (;;) {
      result.add(0, className);

      if ("java/lang/Object".equals(className)) {
        return result;
      }

      String superClass = getDirectSuperClass(className);

      if (superClass == null) {
        log("Can't determine super class of %s", className);

        return null;
      }

      className = superClass;
    }
  }

  /**
   * Returns the value of the direct super class property.
   *
   * @param  className DOCUMENT ME!
   *
   * @return the value of the direct super class property.
   */
  protected String getDirectSuperClass(String className) {
    ClassEntry entry = classes.get(className);

    if (entry != null) {
      return entry.parent;
    }

    String superClass = superClasses.get(className);

    if (superClass == null) {
      superClass = extractSuperClass(className);

      if (superClass != null) {
        superClasses.put(className, superClass);
      }
    }

    return superClass;
  }

  private boolean isJavaCore(String className) {
    return className.startsWith("java/") || className.startsWith("javax/");
  }

  static class ClassEntry {
    final String[] methods;
    final String parent;

    /**
     * Creates a new ClassEntry object.
     *
     * @param parent  DOCUMENT ME!
     * @param methods DOCUMENT ME!
     */
    public ClassEntry(String parent, String ...methods) {
      this.parent = parent;
      this.methods = methods;
    }

    /**
     * Creates a new ClassEntry object.
     *
     * @param iv DOCUMENT ME!
     */
    public ClassEntry(CheckInstrumentationVisitor iv) {
      this(iv.getParent(), iv.getMethodNames());
    }
  }
}
