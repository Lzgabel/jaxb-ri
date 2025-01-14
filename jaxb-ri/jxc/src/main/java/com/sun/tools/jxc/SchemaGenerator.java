/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.jxc;

import com.sun.tools.jxc.ap.Options;
import com.sun.tools.xjc.BadCommandLineException;
import cn.glassfish.jaxb.core.util.Which;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.OptionChecker;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jakarta.xml.bind.JAXBContext;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI entry-point to the schema generator.
 *
 * @author Bhakti Mehta
 */
public class SchemaGenerator {

    private static final Logger LOGGER = Logger.getLogger(SchemaGenerator.class.getName());

    /**
     * Runs the schema generator.
     * @param args command line arguments
     * @throws java.lang.Exception for error
     */
    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }

    /**
     * Runs the schema generator.
     * @param args command line arguments
     * @return
     *      exit code. 0 if success.
     * @throws java.lang.Exception for error
     */
    public static int run(String[] args) throws Exception {
        try {
            ClassLoader cl = SecureLoader.getClassClassLoader(SchemaGenerator.class);
            if (cl==null) {
                cl = SecureLoader.getSystemClassLoader();
            }
            return run(args, cl);
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Runs the schema generator.
     *
     * @param args arguments for the schema generator
     * @param classLoader
     *      the schema generator will run in this classLoader.
     *      It needs to be able to load annotation processing and JAXB RI classes. Note that
     *      JAXB RI classes refer to annotation processing classes. Must not be null.
     *
     * @return
     *      exit code. 0 if success.
     * @throws java.lang.ClassNotFoundException if {@link Runner} cannot be found
     * @throws java.lang.NoSuchMethodException if {@link Runner#compile(java.lang.String[], java.io.File) } cannot be found
     * @throws java.lang.IllegalAccessException if {@link Runner#compile(java.lang.String[], java.io.File) } cannot be accessed
     * @throws java.lang.reflect.InvocationTargetException if {@link Runner#compile(java.lang.String[], java.io.File) } cannot be invoked
     *
     */
    public static int run(String[] args, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Options options = new Options();
        if (args.length ==0) {
            usage();
            return -1;
        }
        for (String arg : args) {
            if (arg.equals("-help")) {
                usage();
                return -1;
            }

            if (arg.equals("-version")) {
                System.out.println(Messages.VERSION.format());
                return -1;
            }

            if (arg.equals("-fullversion")) {
                System.out.println(Messages.FULLVERSION.format());
                return -1;
            }

        }

        try {
            options.parseArguments(args);
        } catch (BadCommandLineException e) {
            // there was an error in the command line.
            // print usage and abort.
            System.out.println(e.getMessage());
            System.out.println();
            usage();
            return -1;
        }

        Class<?> schemagenRunner = classLoader.loadClass(Runner.class.getName());
        Method compileMethod = schemagenRunner.getDeclaredMethod("compile",String[].class,File.class);

        List<String> aptargs = new ArrayList<>();

        if (options.encoding != null) {
            aptargs.add("-encoding");
            aptargs.add(options.encoding);
        }

        aptargs.add("-cp");
        aptargs.add(setClasspath(options.classpath)); // set original classpath + jaxb-api to be visible to annotation processor

        if(options.targetDir!=null) {
            aptargs.add("-d");
            aptargs.add(options.targetDir.getPath());
        }

        aptargs.addAll(options.arguments);

        String[] argsarray = aptargs.toArray(new String[aptargs.size()]);
        return ((Boolean) compileMethod.invoke(null, argsarray, options.episodeFile)) ? 0 : 1;
    }

    private static String setClasspath(String givenClasspath) {
        StringBuilder cp = new StringBuilder();
        appendPath(cp, givenClasspath);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) cl).getURLs()) {
                    try {
                        appendPath(cp,new File(url.toURI()).getPath());
                    } catch(URISyntaxException ex) {
                        /*If the URL is not properly formated - skip it*/
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
            }
            cl = cl.getParent();
        }

        appendPath(cp, findJaxbApiJar());
        return cp.toString();
    }

    private static void appendPath(StringBuilder cp, String url) {
        if (url == null || url.trim().isEmpty())
            return;
        if (cp.length() != 0)
            cp.append(File.pathSeparatorChar);
        cp.append(url);
    }

    /**
     * Computes the file system path of {@code jakarta.xml.bind-api.jar} so that
     * Annotation Processing will see them in the {@code -cp} option.
     *
     * <p>
     * In Java, you can't do this reliably (for that matter there's no guarantee
     * that such a jar file exists, such as in Glassfish), so we do the best we can.
     *
     * @return
     *      null if failed to locate it.
     */
    private static String findJaxbApiJar() {
        String url = Which.which(JAXBContext.class);
        if(url==null)       return null;    // impossible, but hey, let's be defensive

        if(!url.startsWith("jar:") || url.lastIndexOf('!')==-1)
            // no jar file
            return null;

        String jarFileUrl = url.substring(4,url.lastIndexOf('!'));
        if(!jarFileUrl.startsWith("file:"))
            return null;    // not from file system

        try {
            File f = new File(new URL(jarFileUrl).toURI());
            if (f.exists() && f.getName().endsWith(".jar")) { // see 6510966
                return f.getPath();
            }
            f = new File(new URL(jarFileUrl).getFile());
            if (f.exists() && f.getName().endsWith(".jar")) { // this is here for potential backw. compatibility issues
                return f.getPath();
            }
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    private static void usage( ) {
        System.out.println(Messages.USAGE.format());
    }

    public static final class Runner {
        public static boolean compile(String[] args, File episode) throws Exception {

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            JavacOptions options = JavacOptions.parse(compiler, fileManager, args);
            List<String> unrecognizedOptions = options.getUnrecognizedOptions();
            if (!unrecognizedOptions.isEmpty()) {
                LOGGER.log(Level.WARNING, "Unrecognized options found: {0}", unrecognizedOptions);
            }
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(options.getFiles());
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options.getRecognizedOptions(),
                    options.getClassNames(),
                    compilationUnits);
            com.sun.tools.jxc.ap.SchemaGenerator r = new com.sun.tools.jxc.ap.SchemaGenerator();
            if (episode != null)
                r.setEpisodeFile(episode);
            task.setProcessors(Collections.singleton(r));
            boolean res = task.call();
            //Print compiler generated messages
            for( Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics() ) {
                 System.err.println(d.toString());
            }
            return res;
        }
    }

    /**
          *  @author Peter von der Ahe
          */
    private static final class JavacOptions {
        private final List<String> recognizedOptions;
        private final List<String> classNames;
        private final List<File> files;
        private final List<String> unrecognizedOptions;

        private JavacOptions(List<String> recognizedOptions, List<String> classNames, List<File> files,
                             List<String> unrecognizedOptions) {
            this.recognizedOptions = recognizedOptions;
            this.classNames = classNames;
            this.files = files;
            this.unrecognizedOptions = unrecognizedOptions;
        }

        public static JavacOptions parse(OptionChecker primary, OptionChecker secondary, String... arguments) {
            List<String> recognizedOptions = new ArrayList<>();
            List<String> unrecognizedOptions = new ArrayList<>();
            List<String> classNames = new ArrayList<>();
            List<File> files = new ArrayList<>();
            for (int i = 0; i < arguments.length; i++) {
                String argument = arguments[i];
                int optionCount = primary.isSupportedOption(argument);
                if (optionCount < 0) {
                    optionCount = secondary.isSupportedOption(argument);
                }
                if (optionCount < 0) {
                    File file = new File(argument);
                    if (file.exists())
                        files.add(file);
                    else if (SourceVersion.isName(argument))
                        classNames.add(argument);
                    else
                        unrecognizedOptions.add(argument);
                } else {
                    for (int j = 0; j < optionCount + 1; j++) {
                        int index = i + j;
                        if (index == arguments.length) throw new IllegalArgumentException(argument);
                        recognizedOptions.add(arguments[index]);
                    }
                    i += optionCount;
                }
            }
            return new JavacOptions(recognizedOptions, classNames, files, unrecognizedOptions);
        }

        /**
                     * Returns the list of recognized options and their arguments.
                     *
                     * @return a list of options
                     */
        public List<String> getRecognizedOptions() {
            return Collections.unmodifiableList(recognizedOptions);
        }

        /**
                     * Returns the list of file names.
                     *
                     * @return a list of file names
                     */
        public List<File> getFiles() {
            return Collections.unmodifiableList(files);
        }

        /**
                     * Returns the list of class names.
                     *
                     * @return a list of class names
                     */
        public List<String> getClassNames() {
            return Collections.unmodifiableList(classNames);
        }

        /**
                     * Returns the list of unrecognized options.
                     *
                     * @return a list of unrecognized options
                     */
        public List<String> getUnrecognizedOptions() {
            return Collections.unmodifiableList(unrecognizedOptions);
        }

        @Override
        public String toString() {
            return String.format("recognizedOptions = %s; classNames = %s; " + "files = %s; unrecognizedOptions = %s", recognizedOptions, classNames, files, unrecognizedOptions);
        }
    }
}
