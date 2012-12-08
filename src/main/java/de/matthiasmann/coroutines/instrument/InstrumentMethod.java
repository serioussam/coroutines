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

import java.util.List;

import de.matthiasmann.coroutines.Stack;
import de.matthiasmann.coroutines.SuspendExecution;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * Instrument a method to allow suspension.
 *
 * @author Matthias Mann
 */
public class InstrumentMethod {
  private static final String STACK_NAME = Type.getInternalName(Stack.class);

  private final MethodDatabase db;
  private final String className;
  private final MethodNode mn;
  private final Frame<BasicValue>[] frames;
  private final int lvarStack;
  private final int firstLocal;

  private FrameInfo[] codeBlocks = new FrameInfo[32];
  private int numCodeBlocks;
  private int maxObjSlots;

  private boolean warnedAboutMonitors;

  /**
   * Creates a new InstrumentMethod object.
   *
   * @param  db        DOCUMENT ME!
   * @param  className DOCUMENT ME!
   * @param  mn        DOCUMENT ME!
   *
   * @throws AnalyzerException DOCUMENT ME!
   */
  public InstrumentMethod(MethodDatabase db, String className, MethodNode mn)
      throws AnalyzerException {
    this.db = db;
    this.className = className;
    this.mn = mn;

    try {
      Analyzer<BasicValue> a =
        new Analyzer<BasicValue>(new TypeInterpreter(db));

      this.frames = a.analyze(className, mn);
      this.lvarStack = mn.maxLocals;
      this.firstLocal =
        ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
    } catch (UnsupportedOperationException ex) {
      throw new AnalyzerException(null, ex.getMessage(), ex);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public boolean collectCodeBlocks() {
    int numIns = mn.instructions.size();

    maxObjSlots = 0;
    codeBlocks[0] = FrameInfo.FIRST;

    for (int i = 0; i < numIns; i++) {
      Frame<BasicValue> f = frames[i];

      if (f != null) { // reachable ?

        AbstractInsnNode in = mn.instructions.get(i);

        if (in.getType() == AbstractInsnNode.METHOD_INSN) {
          MethodInsnNode min = (MethodInsnNode) in;

          if (db.isMethodSuspendable(min.owner, min.name)) {
            if (db.isDebug()) {
              db.log(
                "Method call at instruction %d to %s#%s  is suspendable", i,
                min.owner, min.name
              );
            }

            FrameInfo fi = addCodeBlock(f, i);

            splitTryCatch(fi);

            if (fi.numObjSlots > maxObjSlots) {
              maxObjSlots = fi.numObjSlots;
            }
          }
        }
      }
    }

    addCodeBlock(null, numIns);

    return numCodeBlocks > 1;
  }

  /**
   * DOCUMENT ME!
   *
   * @param  mv DOCUMENT ME!
   *
   * @throws UnableToInstrumentException DOCUMENT ME!
   */
  public void accept(MethodVisitor mv) {
    mv.visitCode();

    Label lMethodStart = new Label();
    Label lMethodEnd = new Label();
    Label lCatchSEE = new Label();
    Label lCatchAll = new Label();
    Label[] lMethodCalls = new Label[numCodeBlocks - 1];

    for (int i = 1; i < numCodeBlocks; i++) {
      lMethodCalls[i - 1] = new Label();
    }

    mv.visitTryCatchBlock(
      lMethodStart, lMethodEnd, lCatchSEE,
      CheckInstrumentationVisitor.EXCEPTION_NAME
    );

    for (Object o : mn.tryCatchBlocks) {
      TryCatchBlockNode tcb = (TryCatchBlockNode) o;

      if (CheckInstrumentationVisitor.EXCEPTION_NAME.equals(tcb.type)) {
        throw new UnableToInstrumentException(
          "catch for " + SuspendExecution.class.getSimpleName(), className,
          mn.name
        );
      }

      tcb.accept(mv);
    }

    if (mn.visibleParameterAnnotations != null) {
      dumpParameterAnnotations(mv, mn.visibleParameterAnnotations, true);
    }

    if (mn.invisibleParameterAnnotations != null) {
      dumpParameterAnnotations(mv, mn.invisibleParameterAnnotations, false);
    }

    if (mn.visibleAnnotations != null) {
      for (AnnotationNode an : mn.visibleAnnotations) {
        an.accept(mv.visitAnnotation(an.desc, true));
      }
    }

    mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchAll, null);

    mv.visitMethodInsn(
      Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";"
    );
    mv.visitInsn(Opcodes.DUP);
    mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

    mv.visitMethodInsn(
      Opcodes.INVOKEVIRTUAL, STACK_NAME, "nextMethodEntry", "()I"
    );
    mv.visitTableSwitchInsn(1, numCodeBlocks - 1, lMethodStart, lMethodCalls);

    mv.visitLabel(lMethodStart);
    dumpCodeBlock(mv, 0, 0);

    for (int i = 1; i < numCodeBlocks; i++) {
      FrameInfo fi = codeBlocks[i];

      MethodInsnNode min =
        (MethodInsnNode) (mn.instructions.get(fi.endInstruction));

      if (
        InstrumentClass.COROUTINE_NAME.equals(min.owner)
          && "yield".equals(min.name)
      ) {
        // special case - call to yield() - resume AFTER the call
        if (min.getOpcode() != Opcodes.INVOKESTATIC) {
          throw new UnableToInstrumentException(
            "invalid call to yield()", className, mn.name
          );
        }

        emitStoreState(mv, i, fi);
        mv.visitFieldInsn(
          Opcodes.GETSTATIC, STACK_NAME,
          "exception_instance_not_for_user_code",
          CheckInstrumentationVisitor.EXCEPTION_DESC
        );
        mv.visitInsn(Opcodes.ATHROW);
        min.accept(mv); // only the call
        mv.visitLabel(lMethodCalls[i - 1]);
        emitRestoreState(mv, i, fi);
        dumpCodeBlock(mv, i, 1); // skip the call
      } else {
        // normal case - call to a suspendable method - resume before the call
        emitStoreState(mv, i, fi);
        mv.visitLabel(lMethodCalls[i - 1]);
        emitRestoreState(mv, i, fi);
        dumpCodeBlock(mv, i, 0);
      }
    }

    mv.visitLabel(lMethodEnd);

    mv.visitLabel(lCatchAll);
    emitPopMethod(mv);
    mv.visitLabel(lCatchSEE);
    mv.visitInsn(Opcodes.ATHROW); // rethrow shared between catchAll and
                                  // catchSSE

    if (mn.localVariables != null) {
      for (Object o : mn.localVariables) {
        ((LocalVariableNode) o).accept(mv);
      }
    }

    mv.visitMaxs(mn.maxStack + 3, mn.maxLocals + 1);
    mv.visitEnd();
  }

  private FrameInfo addCodeBlock(Frame<BasicValue> f, int end) {
    if (++numCodeBlocks == codeBlocks.length) {
      FrameInfo[] newArray = new FrameInfo[numCodeBlocks * 2];

      System.arraycopy(codeBlocks, 0, newArray, 0, codeBlocks.length);
      codeBlocks = newArray;
    }

    FrameInfo fi = new FrameInfo(f, firstLocal, end);

    codeBlocks[numCodeBlocks] = fi;

    return fi;
  }

  private int getLabelIdx(LabelNode l) {
    int idx;

    if (l instanceof BlockLabelNode) {
      idx = ((BlockLabelNode) l).idx;
    } else {
      idx = mn.instructions.indexOf(l);
    }

    // search for the "real" instruction
    for (;;) {
      int type = mn.instructions.get(idx).getType();

      if (
        (type != AbstractInsnNode.LABEL) && (type != AbstractInsnNode.LINE)
      ) {
        return idx;
      }

      idx++;
    }
  }

  private void splitTryCatch(FrameInfo fi) {
    for (int i = 0; i < mn.tryCatchBlocks.size(); i++) {
      TryCatchBlockNode tcb = mn.tryCatchBlocks.get(i);

      int start = getLabelIdx(tcb.start);
      int end = getLabelIdx(tcb.end);

      if ((start <= fi.endInstruction) && (end >= fi.endInstruction)) {
        // System.out.println("i="+i+" start="+start+" end="+end+"
        // split="+splitIdx+
        // " start="+mn.instructions.get(start)+"
        // end="+mn.instructions.get(end));

        // need to split try/catch around the suspendable call
        if (start == fi.endInstruction) {
          tcb.start = fi.createAfterLabel();
        } else {
          if (end > fi.endInstruction) {
            TryCatchBlockNode tcb2 =
              new TryCatchBlockNode(
                fi.createAfterLabel(), tcb.end, tcb.handler, tcb.type
              );

            mn.tryCatchBlocks.add(i + 1, tcb2);
          }

          tcb.end = fi.createBeforeLabel();
        }
      }
    }
  }

  private void dumpCodeBlock(MethodVisitor mv, int idx, int skip) {
    int start = codeBlocks[idx].endInstruction;
    int end = codeBlocks[idx + 1].endInstruction;

    for (int i = start + skip; i < end; i++) {
      AbstractInsnNode ins = mn.instructions.get(i);

      switch (ins.getOpcode()) {
        case Opcodes.RETURN:
        case Opcodes.ARETURN:
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.DRETURN:
          emitPopMethod(mv);
          break;

        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
          if (!db.isAllowMonitors()) {
            throw new UnableToInstrumentException(
              "synchronisation", className, mn.name
            );
          } else if (!warnedAboutMonitors) {
            warnedAboutMonitors = true;
            db.log(
              "WARNING: Method %s#%s contains synchronisation", className,
              mn.name
            );
          }

          break;
      }

      ins.accept(mv);
    }
  }

  private static void dumpParameterAnnotations(
    MethodVisitor mv, List<AnnotationNode>[] parameterAnnotations,
    boolean visible
  ) {
    for (int i = 0; i < parameterAnnotations.length; i++) {
      if (parameterAnnotations[i] != null) {
        for (AnnotationNode an : parameterAnnotations[i]) {
          an.accept(mv.visitParameterAnnotation(i, an.desc, visible));
        }
      }
    }
  }

  private static void emitConst(MethodVisitor mv, int value) {
    if ((value >= -1) && (value <= 5)) {
      mv.visitInsn(Opcodes.ICONST_0 + value);
    } else if ((byte) value == value) {
      mv.visitIntInsn(Opcodes.BIPUSH, value);
    } else if ((short) value == value) {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    } else {
      mv.visitLdcInsn(value);
    }
  }

  private void emitPopMethod(MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
    emitConst(mv, maxObjSlots);
    mv.visitMethodInsn(
      Opcodes.INVOKEVIRTUAL, STACK_NAME, "popMethod", "(I)V"
    );
  }

  private void emitStoreState(MethodVisitor mv, int idx, FrameInfo fi) {
    Frame<BasicValue> f = frames[fi.endInstruction];

    if (fi.lBefore != null) {
      fi.lBefore.accept(mv);
    }

    mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
    emitConst(mv, idx);
    emitConst(mv, fi.numSlots);
    mv.visitMethodInsn(
      Opcodes.INVOKEVIRTUAL, STACK_NAME, "pushMethodAndReserveSpace",
      "(II)V"
    );

    for (int i = f.getStackSize(); i-- > 0;) {
      BasicValue v = f.getStack(i);

      if (!isNullType(v)) {
        emitStoreValue(mv, v, lvarStack, fi.stackSlotIndices[i]);
      } else {
        if (db.isDebug()) {
          db.log(
            "NULL stack entry: type=%s size=%d", v.getType(), v.getSize()
          );
        }

        mv.visitInsn(Opcodes.POP);
      }
    }

    for (int i = firstLocal; i < f.getLocals(); i++) {
      BasicValue v = f.getLocal(i);

      if (!isNullType(v)) {
        mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), i);
        emitStoreValue(mv, v, lvarStack, fi.localSlotIndices[i]);
      }
    }
  }

  private void emitRestoreState(MethodVisitor mv, int idx, FrameInfo fi) {
    Frame<BasicValue> f = frames[fi.endInstruction];

    for (int i = firstLocal; i < f.getLocals(); i++) {
      BasicValue v = f.getLocal(i);

      if (!isNullType(v)) {
        emitRestoreValue(mv, v, lvarStack, fi.localSlotIndices[i]);
        mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), i);
      } else if (v != BasicValue.UNINITIALIZED_VALUE) {
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, i);
      }
    }

    for (int i = 0; i < f.getStackSize(); i++) {
      BasicValue v = f.getStack(i);

      if (!isNullType(v)) {
        emitRestoreValue(mv, v, lvarStack, fi.stackSlotIndices[i]);
      } else {
        mv.visitInsn(Opcodes.ACONST_NULL);
      }
    }

    if (fi.lAfter != null) {
      fi.lAfter.accept(mv);
    }
  }

  private void emitStoreValue(
    MethodVisitor mv, BasicValue v, int lvarStack, int idx
  )
      throws InternalError, IndexOutOfBoundsException {
    String desc;

    switch (v.getType().getSort()) {
      case Type.OBJECT:
      case Type.ARRAY:
        desc = "(Ljava/lang/Object;L" + STACK_NAME + ";I)V";
        break;

      case Type.BOOLEAN:
      case Type.BYTE:
      case Type.SHORT:
      case Type.CHAR:
      case Type.INT:
        desc = "(IL" + STACK_NAME + ";I)V";
        break;

      case Type.FLOAT:
        desc = "(FL" + STACK_NAME + ";I)V";
        break;

      case Type.LONG:
        desc = "(JL" + STACK_NAME + ";I)V";
        break;

      case Type.DOUBLE:
        desc = "(DL" + STACK_NAME + ";I)V";
        break;

      default:
        throw new InternalError("Unexpected type: " + v.getType());
    }

    mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
    emitConst(mv, idx);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "push", desc);
  }

  private void emitRestoreValue(
    MethodVisitor mv, BasicValue v, int lvarStack, int idx
  ) {
    mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
    emitConst(mv, idx);

    switch (v.getType().getSort()) {
      case Type.OBJECT:

        String internalName = v.getType().getInternalName();

        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject",
          "(I)Ljava/lang/Object;"
        );
        if (!internalName.equals("java/lang/Object")) { // don't cast to
                                                        // Object ;)
          mv.visitTypeInsn(Opcodes.CHECKCAST, internalName);
        }

        break;

      case Type.ARRAY:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject",
          "(I)Ljava/lang/Object;"
        );
        mv.visitTypeInsn(Opcodes.CHECKCAST, v.getType().getDescriptor());
        break;

      case Type.BYTE:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I"
        );
        mv.visitInsn(Opcodes.I2B);
        break;

      case Type.SHORT:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I"
        );
        mv.visitInsn(Opcodes.I2S);
        break;

      case Type.CHAR:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I"
        );
        mv.visitInsn(Opcodes.I2C);
        break;

      case Type.BOOLEAN:
      case Type.INT:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I"
        );
        break;

      case Type.FLOAT:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getFloat", "(I)F"
        );
        break;

      case Type.LONG:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getLong", "(I)J"
        );
        break;

      case Type.DOUBLE:
        mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, STACK_NAME, "getDouble", "(I)D"
        );
        break;

      default:
        throw new InternalError("Unexpected type: " + v.getType());
    }
  }

  static boolean isNullType(BasicValue v) {
    return (v == BasicValue.UNINITIALIZED_VALUE)
      || (v.isReference() && v.getType().getInternalName().equals("null"));
  }

  static class BlockLabelNode extends LabelNode {
    final int idx;

    /**
     * Creates a new BlockLabelNode object.
     *
     * @param idx DOCUMENT ME!
     */
    public BlockLabelNode(int idx) {
      this.idx = idx;
    }
  }

  static class FrameInfo {
    static final FrameInfo FIRST = new FrameInfo(null, 0, 0);

    final int endInstruction;
    final int numSlots;
    final int numObjSlots;
    final int[] localSlotIndices;
    final int[] stackSlotIndices;

    BlockLabelNode lBefore;
    BlockLabelNode lAfter;

    /**
     * Creates a new FrameInfo object.
     *
     * @param f              DOCUMENT ME!
     * @param firstLocal     DOCUMENT ME!
     * @param endInstruction DOCUMENT ME!
     */
    public FrameInfo(
      Frame<BasicValue> f, int firstLocal, int endInstruction
    ) {
      this.endInstruction = endInstruction;

      int idxObj = 0;
      int idxPrim = 0;

      if (f != null) {
        stackSlotIndices = new int[f.getStackSize()];

        for (int i = 0; i < f.getStackSize(); i++) {
          BasicValue v = (BasicValue) f.getStack(i);

          if (!isNullType(v)) {
            if (v.isReference()) {
              stackSlotIndices[i] = idxObj++;
            } else {
              stackSlotIndices[i] = idxPrim++;
            }
          } else {
            stackSlotIndices[i] = -666; // an invalid index ;)
          }
        }

        localSlotIndices = new int[f.getLocals()];

        for (int i = firstLocal; i < f.getLocals(); i++) {
          BasicValue v = (BasicValue) f.getLocal(i);

          if (!isNullType(v)) {
            if (v.isReference()) {
              localSlotIndices[i] = idxObj++;
            } else {
              localSlotIndices[i] = idxPrim++;
            }
          } else {
            localSlotIndices[i] = -666; // an invalid index ;)
          }
        }
      } else {
        stackSlotIndices = null;
        localSlotIndices = null;
      }

      numSlots = Math.max(idxPrim, idxObj);
      numObjSlots = idxObj;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public LabelNode createBeforeLabel() {
      if (lBefore == null) {
        lBefore = new BlockLabelNode(endInstruction);
      }

      return lBefore;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public LabelNode createAfterLabel() {
      if (lAfter == null) {
        lAfter = new BlockLabelNode(endInstruction);
      }

      return lAfter;
    }
  }
}
