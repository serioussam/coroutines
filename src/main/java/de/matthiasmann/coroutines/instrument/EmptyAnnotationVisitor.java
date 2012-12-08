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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An empty {@link AnnotationVisitor} for {@link CheckInstrumentationVisitor}
 * because ASM doesn't accept {@code NULL} as visitor.
 *
 * @author Matthias Mann
 */
public class EmptyAnnotationVisitor extends AnnotationVisitor {
  /** DOCUMENT ME! */
  public static final EmptyAnnotationVisitor instance =
    new EmptyAnnotationVisitor();

  private EmptyAnnotationVisitor() {
    super(Opcodes.ASM4);
  }

  /**
   * DOCUMENT ME!
   *
   * @param name  DOCUMENT ME!
   * @param value DOCUMENT ME!
   */
  public void visit(String name, Object value) {}

  /**
   * DOCUMENT ME!
   *
   * @param  name DOCUMENT ME!
   * @param  desc DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public AnnotationVisitor visitAnnotation(String name, String desc) {
    return this;
  }

  /**
   * DOCUMENT ME!
   *
   * @param  name DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public AnnotationVisitor visitArray(String name) {
    return this;
  }

  /** DOCUMENT ME! */
  public void visitEnd() {}

  /**
   * DOCUMENT ME!
   *
   * @param name  DOCUMENT ME!
   * @param desc  DOCUMENT ME!
   * @param value DOCUMENT ME!
   */
  public void visitEnum(String name, String desc, String value) {}

}
