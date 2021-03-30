/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.esa.orbiprotester;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.orekit.errors.OrekitException;

import eu.esa.orbiprotester.runs.TestRun;
import eu.esa.orbiprotester.runs.TestRunFactory;

/**
 * Orbipro Tester.
 *
 * @author Lucian Barbulescu
 */
public class OrbiproTesterLauncher {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(OrbiproTesterLauncher.class);

    /** The folder that contains the input files. */
    private static String inputFolder = "./input";

    /** The folder that contains the reference files (Files generated with Orbipro). */
    private static String referenceFolder = "./matlab";

    /** The folder that contains the output of the application. */
    private static String outputFolder = "./output";

    /**
     * Constructor.
     */
    private OrbiproTesterLauncher() {
        //Nothing to do.
    }

    /**
     * Program entry point.
     *
     * @param args
     *            program arguments
     */
    public static void main(final String[] args) {
        try {

            // Set the English locale
            Locale.setDefault(Locale.ENGLISH);

            // Create the layout for the log messages
            final PatternLayout layout = new PatternLayout(
                    "%d{ISO8601} %-5p %c{1},%L - %m\n");

            // Add a console appender
            LOGGER.addAppender(new ConsoleAppender(layout));

            // Ensure the log folder exists
            new File("./log").mkdirs();

            // Create the appender for the file
            final DailyRollingFileAppender fileAppender = new DailyRollingFileAppender(
                    layout, "./log/app.log", "'.'yyyy-MM-dd");

            // Add a file appender
            LOGGER.addAppender(fileAppender);

            // Log all messages
            LOGGER.setLevel(Level.ALL);

            // Read the input, output and reference folders, if specified;
            boolean defaultInput = true;
            boolean defaultOutput = true;
            boolean defaultReference = true;
            int paramNum = 0;
            String param;
            final List<File> inputFilesList = new ArrayList<File>();

            while (paramNum < args.length) {
                param = args[paramNum];
                if (param.equalsIgnoreCase("-i")) {
                    // The input folder is specified
                    paramNum++;
                    if (paramNum >= args.length) {
                        // Print erro, usage info and exit application
                        printUsageAndExit("A folder that contains the input files must be specified!");
                    } else {
                        //Read input folder name
                        inputFolder = args[paramNum];
                        defaultInput = false;
                        paramNum++;
                    }
                } else if (param.equalsIgnoreCase("-f")) {
                    // An input file is specified
                    paramNum++;
                    if (paramNum >= args.length) {
                        // Print error, usage info and exit application
                        printUsageAndExit("The name of an input file must be specified!");
                    } else {
                        //Read the input file name
                        inputFilesList.add(new File (args[paramNum]));
                        paramNum++;
                    }
                } else if (param.equalsIgnoreCase("-o")) {
                    // The output folder is specified
                    paramNum++;
                    if (paramNum >= args.length) {
                        // Print error, usage info and exit application
                        printUsageAndExit("An output folder must be specified!");
                    } else {
                        //Read output folder name
                        outputFolder = args[paramNum];
                        defaultOutput = false;
                        paramNum++;
                    }
                } else if (param.equalsIgnoreCase("-m")) {
                    // The reference folder is specified
                    paramNum++;
                    if (paramNum >= args.length) {
                        // Print error, usage info and exit application
                        printUsageAndExit("A folder that contains the reference files must be specified!");
                    } else {
                        //Read reference folder name
                        referenceFolder = args[paramNum];
                        defaultReference = false;
                        paramNum++;
                    }
                } else if (param.equalsIgnoreCase("-h")) {
                    // Print usage info and exit application
                    printUsageAndExit(null);
                } else {
                    // Print error, usage info and exit application
                    printUsageAndExit("Unknown parameter: " + args[paramNum]);
                }
            }

            if (defaultInput && inputFilesList.size() == 0) {
                LOGGER.info("No input folder specified! Using default folder ('./input').");
            }
            if (defaultOutput) {
                LOGGER.info("No output folder specified! Using default folder ('./output').");
            }
            if (defaultReference) {
                LOGGER.info("No reference folder specified! Using default folder ('./matlab').");
            }

            //Read the input folder if one was specified or the default should be used
            if (!defaultInput || inputFilesList.size() == 0) {
                //List all input files
                final File inputFolderFile = new File(OrbiproTesterLauncher.inputFolder);
                if (!inputFolderFile.exists() || !inputFolderFile.isDirectory()) {
                    // Print error, usage info and exit application
                    printUsageAndExit("The input folder " + OrbiproTesterLauncher.inputFolder + " is not valid!");
                }

                // Read the input files
                final File[] inputFiles = inputFolderFile.listFiles(new OrbiproTesterLauncher.InputFileFilter());
                if (inputFiles.length < 1) {
                    // Print error, usage info and exit application
                    printUsageAndExit("There are no input files in the folder " + OrbiproTesterLauncher.inputFolder + "!");
                }

                //Add all input files to the list
                inputFilesList.addAll(Arrays.asList(inputFiles));
            }

            // Check the reference folder
            File referenceFolderFile = new File(OrbiproTesterLauncher.referenceFolder);
            if (!referenceFolderFile.exists() || !referenceFolderFile.isDirectory()) {
                // Print error, usage info and exit application
                printUsageAndExit("The reference folder '" + OrbiproTesterLauncher.referenceFolder + "' is not valid!");
            }

            //Check the output folder
            final File baseOutputFolder = new File(OrbiproTesterLauncher.outputFolder);
            if (baseOutputFolder.exists() && !baseOutputFolder.isDirectory()) {
                // Print error, usage info and exit application
                printUsageAndExit(OrbiproTesterLauncher.outputFolder + " is not a valid folder!");
            } else if (!baseOutputFolder.exists() && !baseOutputFolder.mkdirs()) {
                // Print error, usage info and exit application
                printUsageAndExit("Cannot create the " + "output folder " + OrbiproTesterLauncher.outputFolder + "!");
            }

            // create the real output folder within the base output folder
            final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd_HH-mm-ss-SSS", Locale.ENGLISH);
            final File outputFolderFile = new File(baseOutputFolder, sdf.format(new Date(System.currentTimeMillis())));
            if (!outputFolderFile.mkdirs()) {
                // Print error, usage info and exit application
                printUsageAndExit("Cannot create the output folder " + outputFolderFile.getAbsolutePath() + "!");
            }

            // launch the simulation
            LOGGER.info("Starting tests for " + inputFilesList.size() + " input files!");
            for (File f : inputFilesList) {
                LOGGER.info("Starting test for input file " + f.getName());
                try {
                    new OrbiproTesterLauncher().run(f, outputFolderFile, referenceFolderFile);
                    LOGGER.info("Finished test for input file " + f.getName());
                } catch (OrekitException oe) {
                    LOGGER.error(oe.getLocalizedMessage(), oe);
                } catch (IOException ioe) {
                    LOGGER.error(ioe.getLocalizedMessage(), ioe);
                } catch (NullPointerException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }

            LOGGER.info("All tests finished!");

        } catch (IOException ioe) {
            LOGGER.error(ioe.getLocalizedMessage(), ioe);
        }
    }


    /**
     * Print the specified error, if any, the application command line and then exit the application.
     *
     * @param error The error message. If null no message will be displayed.
     */
    private static void printUsageAndExit(final String error) {
        //Log the error message if present
        if (error != null) {
            LOGGER.error(error);
        }

        //Print the usage info
        LOGGER.info("usage: java -jar orbiprotester.jar [-h] [-i INPUT_FOLDER] [-f INPUT_FILE] [-o OUTPUT_FOLDER] [-m MATLAB_FOLDER]");
        LOGGER.info("Where:");
        LOGGER.info("\t-h \tPrint this information and exit.");
        LOGGER.info("");
        LOGGER.info("\t-i INPUT_FOLDER: \tThe folder that contains the input files.");
        LOGGER.info("\t\t\t\tOnly the files that ends with .in are taken into account.");
        LOGGER.info("\t\t\t\tIf this parameter is not specified, the value './input' is used.");
        LOGGER.info("\t\t\t\tThis folder must exist or the application will end.");
        LOGGER.info("");
        LOGGER.info("\t-f INPUT_FILE: \tAn input file.");
        LOGGER.info("\t\t\t\tThis parameter can be repeated several times.");
        LOGGER.info("");
        LOGGER.info("\t-o OUTPUT_FOLDER: \tThe base folder where the output files will be created.");
        LOGGER.info("\t\t\t\tInside this folder a new one for each application run will be created");
        LOGGER.info("\t\t\t\tIf this parameter is not specified, the value './output' is used.");
        LOGGER.info("\t\t\t\tIf the folder does not exist, it will be created.");
        LOGGER.info("");
        LOGGER.info("\t-m MATLAB_FOLDER: \tThe folder that contains the Orbipro generated files.");
        LOGGER.info("\t\t\t\tIf this parameter is not specified, the value './matlab' is used.");
        LOGGER.info("\t\t\t\tIf the folder cannot be read then no comparison will be performed.");
        LOGGER.info("\n\n");

        //Exit application
        System.exit(0);
    }

    /**
     * Run the test for a given input file.
     *
     * @param input the input file that contains the test data
     * @param baseOutputFolder the output folder
     * @param refFolder the folder that contains the reference file. If null is used no comparison will be performed
     *
     * @throws IOException if there is a file related error
     * @throws OrekitException if there is a propagation error
     */
    private void run(final File input, final File baseOutputFolder, final File refFolder)
        throws IOException, OrekitException {

        final TestRun testRun = TestRunFactory.buildTestRun(input, baseOutputFolder, refFolder, LOGGER);

        //initialise the test
        testRun.readInputData();

        // run the test
        testRun.runTest();

        //finalize the test
        testRun.finalizeTest();
    }

    /**
     * @author lucian
     *
     * Filter for the input files
     */
    private static class InputFileFilter implements FileFilter {

        /** Check if the name of the file given as parameter ends with '.in'.
         * @param file the input file to check
         *
         * @return true if the name of the fie ends with '.in', false otherwise
         *
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(final File file) {
            return file.getName().endsWith(".in");
        }
    }
}
