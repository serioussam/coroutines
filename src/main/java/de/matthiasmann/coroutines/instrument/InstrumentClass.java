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

import de.matthiasmann.coroutines.Coroutine;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

/**
 * Instrument a class by instrumenting all suspendable methods and copying the
 * others.
 *
 * @author Matthias Mann
 */
public class InstrumentClass extends ClassVisitor {
  static final String COROUTINE_NAME = Type.getInternalName(Coroutine.class);
  static final String ALREADY_INSTRUMENTED_NAME =
    Type.getDescriptor(AlreadyInstrumented.class);

  private final MethodDatabase db;
  private String className;

  /**
   * Creates a new InstrumentClass object.
   *
   * @param cv DOCUMENT ME!
   * @param db DOCUMENT ME!
   */
  public InstrumentClass(ClassVisitor cv, MethodDatabase db) {
    super(Opcodes.ASM4, cv);

    this.db = db;
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
  @Override
  public void visit(
    int version, int access, String name, String signature, String superName,
    String[] interfaces
  ) {
    this.className = name;

    // need atleast 1.5 for annotations to work
    if (version < Opcodes.V1_5) {
      version = Opcodes.V1_5;
    }

    super.visit(version, access, name, signature, superName, interfaces);
    super.visitAnnotation(ALREADY_INSTRUMENTED_NAME, true);
  }

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
   * @throws InternalError DOCUMENT ME!
   */
  @Override
  public MethodVisitor visitMethod(
    int access, String name, String desc, String signature,
    String[] exceptions
  ) {
    final MethodVisitor outMV =
      super.visitMethod(access, name, desc, signature, exceptions);

    if (
      checkAccess(access)
        && CheckInstrumentationVisitor.checkExceptions(exceptions)
        && !(className.equals(COROUTINE_NAME) && name.equals("yield"))
    ) {
      if (db.isDebug()) {
        db.log("Instrumenting method %s#%s", className, name);
      }

      return new MethodNode(access, name, desc, signature, exceptions) {
        @Override
        public void visitEnd() {
          super.visitEnd();

          try {
            InstrumentMethod im = new InstrumentMethod(db, className, this);

            if (im.collectCodeBlocks()) {
              if (name.charAt(0) == '<') {
                throw new UnableToInstrumentException(
                  "special method", className, name
                );
              }

              im.accept(outMV);
            } else {
              accept(outMV);
            }
          } catch (AnalyzerException ex) {
            ex.printStackTrace();
            throw new InternalError(ex.getMessage());
          }

        }
      };
    }

    return outMV;
  }

  private static boolean checkAccess(int access) {
    return (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
  }
}
