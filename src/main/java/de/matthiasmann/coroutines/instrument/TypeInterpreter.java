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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * An extension to {@link BasicInterpreter} which collects the type of objects
 * and arrays.
 *
 * @author Matthias Mann
 */
public class TypeInterpreter extends BasicInterpreter {
  private final MethodDatabase db;

  /**
   * Creates a new TypeInterpreter object.
   *
   * @param db DOCUMENT ME!
   */
  public TypeInterpreter(MethodDatabase db) {
    this.db = db;
  }

  /**
   * DOCUMENT ME!
   *
   * @param  type DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  @Override
  public BasicValue newValue(Type type) {
    if (type == null) {
      return BasicValue.UNINITIALIZED_VALUE;
    }

    if ((type.getSort() == Type.OBJECT) || (type.getSort() == Type.ARRAY)) {
      return new BasicValue(type);
    }

    return super.newValue(type);
  }

  /**
   * DOCUMENT ME!
   *
   * @param  insn   DOCUMENT ME!
   * @param  value1 DOCUMENT ME!
   * @param  value2 DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws AnalyzerException DOCUMENT ME!
   */
  @Override
  public BasicValue binaryOperation(
    AbstractInsnNode insn, BasicValue value1, BasicValue value2
  )
      throws AnalyzerException {
    if (insn.getOpcode() == Opcodes.AALOAD) {
      BasicValue v1 = (BasicValue) value1;

      assert v1.isReference(): "AALOAD needs an array as first parameter";

      Type t1 = v1.getType();

      assert t1.getSort() == Type.ARRAY: "AALOAD needs an array as first parameter";

      Type resultType = Type.getType(t1.getDescriptor().substring(1));

      return new BasicValue(resultType);
    }

    return super.binaryOperation(insn, value1, value2);
  }

  /**
   * DOCUMENT ME!
   *
   * @param  v DOCUMENT ME!
   * @param  w DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws UnsupportedOperationException DOCUMENT ME!
   */
  @Override
  public BasicValue merge(BasicValue v, BasicValue w) {
    if (!v.equals(w)) {
      if (isNull(v) && (w != BasicValue.UNINITIALIZED_VALUE)) {
        return w;
      }

      if (isNull(w) && (v != BasicValue.UNINITIALIZED_VALUE)) {
        return v;
      }

      if (
        (v != BasicValue.UNINITIALIZED_VALUE)
          && (w != BasicValue.UNINITIALIZED_VALUE)
      ) {
        Type typeA = (v).getType();
        Type typeB = (w).getType();

        if (
          (typeA.getSort() != Type.OBJECT) || (typeB.getSort()
              != Type.OBJECT)
        ) {
          throw new UnsupportedOperationException(
            "merge needs common super class search: v=" + v + " w=" + w
          );
        }

        String internalA = typeA.getInternalName();
        String internalB = typeB.getInternalName();
        String superClass = db.getCommonSuperClass(internalA, internalB);

        if (superClass == null) {
          if (db.isException(internalB)) {
            db.log(
              "Could not determine super class for v=%s w=%s - decided to use exception %s",
              v, w, w
            );

            return w;
          }

          if (db.isException(internalA)) {
            db.log(
              "Could not determine super class for v=%s w=%s - decided to use exception %s",
              v, w, v
            );

            return v;
          }

          throw new UnsupportedOperationException(
            "Unable to resolve common super class: v=" + v + " w=" + w
          );
        }

        db.log("common super class for v=%s w=%s is %s", v, w, superClass);

        return new BasicValue(Type.getType("L" + superClass + ";"));
      }

      return BasicValue.UNINITIALIZED_VALUE;
    }

    return v;
  }

  private static boolean isNull(BasicValue v) {
    return v.isReference() && v.getType().getInternalName().equals("null");
  }
}
