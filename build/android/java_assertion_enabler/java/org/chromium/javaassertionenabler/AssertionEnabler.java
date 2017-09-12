// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.javaassertionenabler;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * An application that replace Java ASSERT statements with a function by modifying Java bytecode. It
 * takes in a JAR file, modifies bytecode of classes that use ASSERT, and outputs the bytecode to a
 * new JAR file.
 *
 * We do this in two steps, first step is to enable assert.
 * Following bytecode is generated for each class with ASSERT statements:
 * 0: ldc #8 // class CLASSNAME
 * 2: invokevirtual #9 // Method java/lang/Class.desiredAssertionStatus:()Z
 * 5: ifne 12
 * 8: iconst_1
 * 9: goto 13
 * 12: iconst_0
 * 13: putstatic #2 // Field $assertionsDisabled:Z
 * Replaces line #13 to the following:
 * 13: pop
 * Consequently, $assertionsDisabled is assigned the default value FALSE.
 * This is done in the first if statement in overridden visitFieldInsn. We do this per per-assert.
 *
 * Second step is to replace assert statement with a function:
 * The followed instructions are generated by a java assert statement:
 * getstatic     #3     // Field $assertionsDisabled:Z
 * ifne          118    // Jump to instruction as if assertion if not enabled
 * ...
 * ifne          19
 * new           #4     // class java/lang/AssertionError
 * dup
 * ldc           #5     // String (don't have this line if no assert message given)
 * invokespecial #6     // Method java/lang/AssertionError.
 * athrow
 * Replace athrow with:
 * invokestatic  #7     // Method org/chromium/base/JavaExceptionReporter.assertFailureHandler
 * goto          118
 * JavaExceptionReporter.assertFailureHandler is a function that handles the AssertionError,
 * 118 is the instruction to execute as if assertion if not enabled.
 */
class AssertionEnabler {
    static final String CLASS_FILE_SUFFIX = ".class";
    static final String TEMPORARY_FILE_SUFFIX = ".temp";
    static final int BUFFER_SIZE = 16384;

    static class AssertionEnablerVisitor extends ClassVisitor {
        AssertionEnablerVisitor(ClassWriter writer) {
            super(Opcodes.ASM5, writer);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, String desc,
                String signature, String[] exceptions) {
            return new RewriteAssertMethodVisitorWriter(
                    Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions));
        }
    }

    static class RewriteAssertMethodVisitorWriter extends MethodVisitor {
        static final String ASSERTION_DISABLED_NAME = "$assertionsDisabled";
        static final String INSERT_INSTRUCTION_OWNER = "org/chromium/build/BuildHooks";
        static final String INSERT_INSTRUCTION_NAME = "assertFailureHandler";
        static final String INSERT_INSTRUCTION_DESC = "(Ljava/lang/AssertionError;)V";
        static final boolean INSERT_INSTRUCTION_ITF = false;

        boolean mStartLoadingAssert;
        Label mGotoLabel;

        public RewriteAssertMethodVisitorWriter(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.PUTSTATIC && name.equals(ASSERTION_DISABLED_NAME)) {
                super.visitInsn(Opcodes.POP); // enable assert
            } else if (opcode == Opcodes.GETSTATIC && name.equals(ASSERTION_DISABLED_NAME)) {
                mStartLoadingAssert = true;
                super.visitFieldInsn(opcode, owner, name, desc);
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            if (mStartLoadingAssert && opcode == Opcodes.IFNE && mGotoLabel == null) {
                mGotoLabel = label;
            }
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitInsn(int opcode) {
            if (!mStartLoadingAssert || opcode != Opcodes.ATHROW) {
                super.visitInsn(opcode);
            } else {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, INSERT_INSTRUCTION_OWNER,
                        INSERT_INSTRUCTION_NAME, INSERT_INSTRUCTION_DESC, INSERT_INSTRUCTION_ITF);
                super.visitJumpInsn(Opcodes.GOTO, mGotoLabel);
                mStartLoadingAssert = false;
                mGotoLabel = null;
            }
        }
    }

    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int numRead = 0;
        byte[] data = new byte[BUFFER_SIZE];
        while ((numRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, numRead);
        }
        return buffer.toByteArray();
    }

    static void enableAssertionInJar(String inputJarPath, String outputJarPath) {
        String tempJarPath = outputJarPath + TEMPORARY_FILE_SUFFIX;
        try (ZipInputStream inputStream = new ZipInputStream(
                     new BufferedInputStream(new FileInputStream(inputJarPath)));
                ZipOutputStream tempStream = new ZipOutputStream(
                        new BufferedOutputStream(new FileOutputStream(tempJarPath)))) {
            ZipEntry entry = null;

            while ((entry = inputStream.getNextEntry()) != null) {
                byte[] byteCode = readAllBytes(inputStream);

                if (entry.isDirectory() || !entry.getName().endsWith(CLASS_FILE_SUFFIX)) {
                    tempStream.putNextEntry(entry);
                    tempStream.write(byteCode);
                    tempStream.closeEntry();
                    continue;
                }
                ClassReader reader = new ClassReader(byteCode);
                ClassWriter writer = new ClassWriter(reader, 0);
                reader.accept(new AssertionEnablerVisitor(writer), 0);
                byte[] patchedByteCode = writer.toByteArray();
                tempStream.putNextEntry(new ZipEntry(entry.getName()));
                tempStream.write(patchedByteCode);
                tempStream.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Path src = Paths.get(tempJarPath);
            Path dest = Paths.get(outputJarPath);
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments.");
            System.out.println("Example usage: java_assertion_enabler input.jar output.jar");
            System.exit(-1);
        }
        String inputJarPath = args[0];
        String outputJarPath = args[1];
        enableAssertionInJar(inputJarPath, outputJarPath);
    }
}
