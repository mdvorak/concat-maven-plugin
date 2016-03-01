package org.mdvorak;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.*;
import java.util.*;

/**
 * Goal which concatenates several files and creates a new file as specified.
 */
@Mojo(name = "concat", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class ConcatMojo extends AbstractMojo {

    /**
     * Base directory for the files.
     */
    @Parameter(property = "project.basedir")
    private File sourceDirectory;

    /**
     * The resulting file
     */
    @Parameter(required = true)
    private File outputFile;

    /**
     * Files to concatenate. It supports ant-like file masks. When there is a mask specified,
     * order of matched files is unspecified.
     */
    @Parameter(required = true)
    private List<String> concatFiles;

    @Parameter(property = "project.build.sourceEncoding")
    private String sourceEncoding;

    /**
     * Append newline after each concatenation
     */
    @Parameter
    private boolean appendNewline = false;


    /* (non-Javadoc)
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException {
        if (validate()) {
            getLog().debug("Going to concatenate files to destination file: " + outputFile.getAbsolutePath());

            final char[] buffer = new char[4096];
            Writer outputWriter = null;

            try {
                outputWriter = new OutputStreamWriter(new FileOutputStream(outputFile), sourceEncoding);

                for (String file : collectFiles()) {
                    File inputFile = new File(sourceDirectory, file);

                    getLog().debug("Appending file: " + inputFile.getAbsolutePath());

                    Reader inputReader = null;
                    try {
                        inputReader = new InputStreamReader(new FileInputStream(inputFile), sourceEncoding);
                        IOUtils.copyLarge(inputReader, outputWriter, buffer);
                    } finally {
                        IOUtils.closeQuietly(inputReader);
                    }

                    if (appendNewline) {
                        outputWriter.write(System.getProperty("line.separator"));
                    }
                }

                // Don't consume exception
                outputWriter.close();
                outputWriter = null;
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to concatenate", e);
            } finally {
                IOUtils.closeQuietly(outputWriter);
            }
        }
    }

    private boolean validate() throws MojoExecutionException {
        if (outputFile == null) {
            throw new MojoExecutionException("Please specify a correct outputFile");
        }

        if (concatFiles == null || concatFiles.size() == 0) {
            throw new MojoExecutionException("Please specify the file(s) to concatenate");
        }

        if (sourceDirectory == null || !sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("sourceDirectory " + String.valueOf(sourceDirectory) + " does not exist");
        }

        return true;
    }


    protected Collection<String> collectFiles() throws MojoExecutionException {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.addDefaultExcludes();

        scanner.setBasedir(sourceDirectory);

        // Preserve order
        Collection<String> sources = new LinkedHashSet<String>();

        for (String include : concatFiles) {
            scanner.setIncludes(new String[]{include});
            scanner.scan();

            if (scanner.getIncludedFiles().length < 1) {
                throw new MojoExecutionException("Pattern " + include + " did not match any files in directory " + sourceDirectory);
            }

            // Sort within the include mask
            final List<String> includedFiles = new ArrayList<String>(Arrays.asList(scanner.getIncludedFiles()));
            Collections.sort(includedFiles);

            sources.addAll(includedFiles);
        }

        return Collections.unmodifiableCollection(sources);
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setConcatFiles(List<String> concatFiles) {
        this.concatFiles = concatFiles;
    }

    public void setAppendNewline(boolean appendNewline) {
        this.appendNewline = appendNewline;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }
}
