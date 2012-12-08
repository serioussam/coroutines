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
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;

import de.matthiasmann.coroutines.SuspendExecution;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * <p>Instrumentation ANT task</p>
 *
 * <p>It requires one or more FileSet elements pointing to class files that
 * should be instrumented.</p>
 *
 * <p>Classes that are referenced from the instrumented classes are searched
 * in the classpath of the task. If a referenced class is not found a warning
 * is generated and the instrumentation will result in less efficent code.</p>
 *
 * <p>The following options can be set:</p>
 *
 * <ul>
 * <li>check - default: false<br/>
 * The resulting code is run through a verifier.</li>
 * <li>verbose - default: false<br/>
 * The name of each processed class is displayed.</li>
 * </ul>
 *
 * @see    <a href="http://ant.apache.org/manual/CoreTypes/fileset.html">ANT
 *         FileSet</a>
 * @see    SuspendExecution
 * @author Matthias Mann
 */
public class InstrumentationTask extends Task {
  private ArrayList<FileSet> filesets = new ArrayList<FileSet>();
  private boolean check;
  private boolean verbose;
  private boolean allowMonitors;
  private boolean debug;

  /**
   * DOCUMENT ME!
   *
   * @param fs DOCUMENT ME!
   */
  public void addFileSet(FileSet fs) {
    filesets.add(fs);
  }

  /**
   * Sets the value of the check property.
   *
   * @param check the new value of the check property.
   */
  public void setCheck(boolean check) {
    this.check = check;
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
   * Sets the value of the allow monitors property.
   *
   * @param allowMonitors the new value of the allow monitors property.
   */
  public void setAllowMonitors(boolean allowMonitors) {
    this.allowMonitors = allowMonitors;
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
   * @throws BuildException DOCUMENT ME!
   */
  @Override
  public void execute()
      throws BuildException {
    MethodDatabase db = new MethodDatabase(getClass().getClassLoader());

    db.setVerbose(verbose);
    db.setDebug(debug);
    db.setAllowMonitors(allowMonitors);
    db.setLog(
      new Log() {
        public void log(String msg, Object ...args) {
          InstrumentationTask.this.log(String.format(msg, args));
        }

        public void error(String msg, Exception ex) {
          InstrumentationTask.this.log(msg, ex, Project.MSG_ERR);
        }
      }
    );

    try {
      for (FileSet fs : filesets) {
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();

        for (String filename : includedFiles) {
          File file = new File(fs.getDir(), filename);

          if (file.isFile()) {
            db.checkClass(file);
          } else {
            log("File not found: " + filename);
          }
        }
      }

      log("Instrumenting " + db.getWorkList().size() + " classes");

      for (File f : db.getWorkList()) {
        instrumentClass(db, f);
      }
    } catch (UnableToInstrumentException ex) {
      log(ex.getMessage());
      throw new BuildException(ex.getMessage(), ex);
    }
  }

  private void instrumentClass(MethodDatabase db, File f) {
    if (verbose) {
      log("Instrumenting class " + f);
    }

    try {
      ClassReader r;

      FileInputStream fis = new FileInputStream(f);

      try {
        r = new ClassReader(fis);
      } finally {
        fis.close();
      }

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      ClassVisitor cv = check ? new CheckClassAdapter(cw) : cw;
      InstrumentClass ic = new InstrumentClass(cv, db);

      r.accept(ic, ClassReader.SKIP_FRAMES);

      byte[] newClass = cw.toByteArray();

      FileOutputStream fos = new FileOutputStream(f);

      try {
        fos.write(newClass);
      } finally {
        fos.close();
      }
    } catch (IOException ex) {
      throw new BuildException("Instrumenting file " + f, ex);
    }
  }
}
