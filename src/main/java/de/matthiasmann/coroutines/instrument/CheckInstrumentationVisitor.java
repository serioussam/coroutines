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

import java.util.ArrayList;

import de.matthiasmann.coroutines.SuspendExecution;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Check if a class contains suspendable methods. Basicly this class checks if
 * a method is declared to throw {@link SuspendExecution}.
 *
 * @author Matthias Mann
 */
public class CheckInstrumentationVisitor extends ClassVisitor {
  static final String EXCEPTION_NAME =
    Type.getInternalName(SuspendExecution.class);
  static final String EXCEPTION_DESC =
    Type.getDescriptor(SuspendExecution.class);
  static final String[] EMPTY_METHOD_NAMES = {};

  private ArrayList<String> methods;
  private String className;
  private String parent;
  private boolean alreadyInstrumented;

  /** Creates a new CheckInstrumentationVisitor object. */
  public CheckInstrumentationVisitor() {
    super(Opcodes.ASM4);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public boolean needsInstrumentation() {
    return methods != null;
  }

  /**
   * Returns the value of the method names property.
   *
   * @return the value of the method names property.
   */
  public String[] getMethodNames() {
    if (methods == null) {
      return EMPTY_METHOD_NAMES;
    }

    return methods.toArray(new String[methods.size()]);
  }

  /**
   * Returns the value of the name property.
   *
   * @return the value of the name property.
   */
  public String getName() {
    return className;
  }

  /**
   * Returns the value of the parent property.
   *
   * @return the value of the parent property.
   */
  public String getParent() {
    return parent;
  }

  /**
   * Returns the value of the already instrumented property.
   *
   * @return the value of the already instrumented property.
   */
  public boolean isAlreadyInstrumented() {
    return alreadyInstrumented;
  }

  /**
   * DOCUMENT ME!
   *
   * @param version    DOCUMENT ME!
   * @param access     DOCUMENT ME!
   * @param name       DOCUMENT ME!
   * @param signature  DOCUMENT ME!
   * @param superName  DOCUMENT ME!
   * @param interfaces DOCUMENT ME!
   */
  public void visit(
    int version, int access, String name, String signature, String superName,
    String[] interfaces
  ) {
    this.className = name;
    this.parent = superName;
  }

  /**
   * DOCUMENT ME!
   *
   * @param  desc    DOCUMENT ME!
   * @param  visible DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    if (desc.equals(InstrumentClass.ALREADY_INSTRUMENTED_NAME)) {
      alreadyInstrumented = true;
    }

    return EmptyAnnotationVisitor.instance;
  }

  /**
   * DOCUMENT ME!
   *
   * @param attr DOCUMENT ME!
   */
  public void visitAttribute(Attribute attr) {}

  /** DOCUMENT ME! */
  public void visitEnd() {}

  /**
   * DOCUMENT ME!
   *
   * @param  access    DOCUMENT ME!
   * @param  name      DOCUMENT ME!
   * @param  desc      DOCUMENT ME!
   * @param  signature DOCUMENT ME!
   * @param  value     DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public FieldVisitor visitField(
    int access, String name, String desc, String signature, Object value
  ) {
    return null;
  }

  /**
   * DOCUMENT ME!
   *
   * @param name      DOCUMENT ME!
   * @param outerName DOCUMENT ME!
   * @param innerName DOCUMENT ME!
   * @param access    DOCUMENT ME!
   */
  public void visitInnerClass(
    String name, String outerName, String innerName, int access
  ) {}

  /**
   * DOCUMENT ME!
   *
   * @param  access     DOCUMENT ME!
   * @param  name       DOCUMENT ME!
   * @param  desc       DOCUMENT ME!
   * @param  signature  DOCUMENT ME!
   * @param  exceptions DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws UnableToInstrumentException DOCUMENT ME!
   */
  public MethodVisitor visitMethod(
    int access, String name, String desc, String signature,
    String[] exceptions
  ) {
    if (checkExceptions(exceptions)) {
      // synchronized methods can't be made suspendable
      if ((access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED) {
        throw new UnableToInstrumentException(
          "synchronized method", className, name
        );
      }

      if (methods == null) {
        methods = new ArrayList<String>();
      }

      methods.add(name);
    }

    return null;
  }

  /**
   * DOCUMENT ME!
   *
   * @param owner DOCUMENT ME!
   * @param name  DOCUMENT ME!
   * @param desc  DOCUMENT ME!
   */
  public void visitOuterClass(String owner, String name, String desc) {}

  /**
   * DOCUMENT ME!
   *
   * @param source DOCUMENT ME!
   * @param debug  DOCUMENT ME!
   */
  public void visitSource(String source, String debug) {}

  /**
   * DOCUMENT ME!
   *
   * @param  exceptions DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public static boolean checkExceptions(String[] exceptions) {
    if (exceptions != null) {
      for (String ex : exceptions) {
        if (ex.equals(EXCEPTION_NAME)) {
          return true;
        }
      }
    }

    return false;
  }

}
