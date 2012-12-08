/*
 * Copyright (c) 2008, Matthias Mann
 *
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
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.coroutines.instrument;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** @author Matthias Mann */
public class ExtractSuperClass extends ClassVisitor {
  String superClass;

  /** Creates a new ExtractSuperClass object. */
  public ExtractSuperClass() {
    super(Opcodes.ASM4);
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
    this.superClass = superName;
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
    return null;
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
   */
  public MethodVisitor visitMethod(
    int access, String name, String desc, String signature,
    String[] exceptions
  ) {
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

}
