/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.compress.utils.ServiceLoaderIterator;
import org.apache.commons.compress.utils.Sets;

/**
 * <p>
 * Factory to create Compressor[In|Out]putStreams from names. To add other
 * implementations you should extend CompressorStreamFactory and override the
 * appropriate methods (and call their implementation from super of course).
 * </p>
 * 
 * Example (Compressing a file):
 * 
 * <pre>
 * final OutputStream out = new FileOutputStream(output);
 * CompressorOutputStream cos = new CompressorStreamFactory()
 *         .createCompressorOutputStream(CompressorStreamFactory.BZIP2, out);
 * IOUtils.copy(new FileInputStream(input), cos);
 * cos.close();
 * </pre>
 * 
 * Example (Decompressing a file):
 * 
 * <pre>
 * final InputStream is = new FileInputStream(input);
 * CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2,
 *         is);
 * IOUtils.copy(in, new FileOutputStream(output));
 * in.close();
 * </pre>
 * 
 * @Immutable provided that the deprecated method setDecompressConcatenated is
 *            not used.
 * @ThreadSafe even if the deprecated method setDecompressConcatenated is used
 */
public class CompressorStreamFactory implements CompressorStreamProvider {

    private static final CompressorStreamFactory SINGLETON = new CompressorStreamFactory();

    /**
     * Constant (value {@value}) used to identify the BZIP2 compression
     * algorithm.
     * 
     * @since 1.1
     */
    public static final String BZIP2 = "bzip2";

    /**
     * Constant (value {@value}) used to identify the GZIP compression
     * algorithm.
     * 
     * @since 1.1
     */
    public static final String GZIP = "gz";

    /**
     * Constant (value {@value}) used to identify the PACK200 compression
     * algorithm.
     * 
     * @since 1.3
     */
    public static final String PACK200 = "pack200";

    /**
     * Constant (value {@value}) used to identify the XZ compression method.
     * 
     * @since 1.4
     */
    public static final String XZ = "xz";

    /**
     * Constant (value {@value}) used to identify the LZMA compression method.
     * 
     * @since 1.6
     */
    public static final String LZMA = "lzma";

    /**
     * Constant (value {@value}) used to identify the "framed" Snappy
     * compression method.
     * 
     * @since 1.7
     */
    public static final String SNAPPY_FRAMED = "snappy-framed";

    /**
     * Constant (value {@value}) used to identify the "raw" Snappy compression
     * method. Not supported as an output stream type.
     * 
     * @since 1.7
     */
    public static final String SNAPPY_RAW = "snappy-raw";

    /**
     * Constant (value {@value}) used to identify the traditional Unix compress
     * method. Not supported as an output stream type.
     * 
     * @since 1.7
     */
    public static final String Z = "z";

    /**
     * Constant (value {@value}) used to identify the Deflate compress method.
     * 
     * @since 1.9
     */
    public static final String DEFLATE = "deflate";

    /**
     * Constant (value {@value}) used to identify the block LZ4
     * compression method. Not supported as an output stream type.
     *
     * @since 1.14
     */
    public static final String LZ4_BLOCK = "lz4-block";

    /**
     * Constructs a new sorted map from input stream provider names to provider
     * objects.
     *
     * <p>
     * The map returned by this method will have one entry for each provider for
     * which support is available in the current Java virtual machine. If two or
     * more supported provider have the same name then the resulting map will
     * contain just one of them; which one it will contain is not specified.
     * </p>
     *
     * <p>
     * The invocation of this method, and the subsequent use of the resulting
     * map, may cause time-consuming disk or network I/O operations to occur.
     * This method is provided for applications that need to enumerate all of
     * the available providers, for example to allow user provider selection.
     * </p>
     *
     * <p>
     * This method may return different results at different times if new
     * providers are dynamically made available to the current Java virtual
     * machine.
     * </p>
     *
     * @return An immutable, map from names to provider objects
     * @since 1.13
     */
    public static SortedMap<String, CompressorStreamProvider> findAvailableCompressorInputStreamProviders() {
        return AccessController.doPrivileged(new PrivilegedAction<SortedMap<String, CompressorStreamProvider>>() {
            @Override
            public SortedMap<String, CompressorStreamProvider> run() {
                final TreeMap<String, CompressorStreamProvider> map = new TreeMap<>();
                putAll(SINGLETON.getInputStreamCompressorNames(), SINGLETON, map);
                for (final CompressorStreamProvider provider : findCompressorStreamProviders()) {
                    putAll(provider.getInputStreamCompressorNames(), provider, map);
                }
                return map;
            }
        });
    }

    /**
     * Constructs a new sorted map from output stream provider names to provider
     * objects.
     *
     * <p>
     * The map returned by this method will have one entry for each provider for
     * which support is available in the current Java virtual machine. If two or
     * more supported provider have the same name then the resulting map will
     * contain just one of them; which one it will contain is not specified.
     * </p>
     *
     * <p>
     * The invocation of this method, and the subsequent use of the resulting
     * map, may cause time-consuming disk or network I/O operations to occur.
     * This method is provided for applications that need to enumerate all of
     * the available providers, for example to allow user provider selection.
     * </p>
     *
     * <p>
     * This method may return different results at different times if new
     * providers are dynamically made available to the current Java virtual
     * machine.
     * </p>
     *
     * @return An immutable, map from names to provider objects
     * @since 1.13
     */
    public static SortedMap<String, CompressorStreamProvider> findAvailableCompressorOutputStreamProviders() {
        return AccessController.doPrivileged(new PrivilegedAction<SortedMap<String, CompressorStreamProvider>>() {
            @Override
            public SortedMap<String, CompressorStreamProvider> run() {
                final TreeMap<String, CompressorStreamProvider> map = new TreeMap<>();
                putAll(SINGLETON.getOutputStreamCompressorNames(), SINGLETON, map);
                for (final CompressorStreamProvider provider : findCompressorStreamProviders()) {
                    putAll(provider.getOutputStreamCompressorNames(), provider, map);
                }
                return map;
            }

        });
    }
    private static ArrayList<CompressorStreamProvider> findCompressorStreamProviders() {
        return Lists.newArrayList(serviceLoaderIterator());
    }
    
    public static String getBzip2() {
        return BZIP2;
    }

    public static String getDeflate() {
        return DEFLATE;
    }

    public static String getGzip() {
        return GZIP;
    }

    public static String getLzma() {
        return LZMA;
    }

    public static String getPack200() {
        return PACK200;
    }

    public static CompressorStreamFactory getSingleton() {
        return SINGLETON;
    }

    public static String getSnappyFramed() {
        return SNAPPY_FRAMED;
    }

    public static String getSnappyRaw() {
        return SNAPPY_RAW;
    }

    public static String getXz() {
        return XZ;
    }

    public static String getZ() {
        return Z;
    }

    static void putAll(final Set<String> names, final CompressorStreamProvider provider,
            final TreeMap<String, CompressorStreamProvider> map) {
        for (final String name : names) {
            map.put(toKey(name), provider);
        }
    }

    private static Iterator<CompressorStreamProvider> serviceLoaderIterator() {
        return new ServiceLoaderIterator<>(CompressorStreamProvider.class);
    }

    private static String toKey(final String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    /**
     * If true, decompress until the end of the input. If false, stop after the
     * first stream and leave the input position to point to the next byte after
     * the stream
     */
    private final Boolean decompressUntilEOF;
    // This is Boolean so setDecompressConcatenated can determine whether it has
    // been set by the ctor
    // once the setDecompressConcatenated method has been removed, it can revert
    // to boolean

    private SortedMap<String, CompressorStreamProvider> compressorInputStreamProviders;

    private SortedMap<String, CompressorStreamProvider> compressorOutputStreamProviders;
    
    /**
     * If true, decompress until the end of the input. If false, stop after the
     * first stream and leave the input position to point to the next byte after
     * the stream
     */
    private volatile boolean decompressConcatenated = false;

    /**
     * Create an instance with the decompress Concatenated option set to false.
     */
    public CompressorStreamFactory() {
        this.decompressUntilEOF = null;
    }

    /**
     * Create an instance with the provided decompress Concatenated option.
     * 
     * @param decompressUntilEOF
     *            if true, decompress until the end of the input; if false, stop
     *            after the first stream and leave the input position to point
     *            to the next byte after the stream. This setting applies to the
     *            gzip, bzip2 and xz formats only.
     * @since 1.10
     */
    public CompressorStreamFactory(final boolean decompressUntilEOF) {
        this.decompressUntilEOF = Boolean.valueOf(decompressUntilEOF);
        // Also copy to existing variable so can continue to use that as the
        // current value
        this.decompressConcatenated = decompressUntilEOF;
    }

    /**
     * Create an compressor input stream from an input stream, autodetecting the
     * compressor type from the first few bytes of the stream. The InputStream
     * must support marks, like BufferedInputStream.
     * 
     * @param in
     *            the input stream
     * @return the compressor input stream
     * @throws CompressorException
     *             if the compressor name is not known
     * @throws IllegalArgumentException
     *             if the stream is null or does not support mark
     * @since 1.1
     */
    public CompressorInputStream createCompressorInputStream(final InputStream in) throws CompressorException {
        if (in == null) {
            throw new IllegalArgumentException("Stream must not be null.");
        }

        if (!in.markSupported()) {
            throw new IllegalArgumentException("Mark is not supported.");
        }

        final byte[] signature = new byte[12];
        in.mark(signature.length);
        try {
            final int signatureLength = IOUtils.readFully(in, signature);
            in.reset();

            if (BZip2CompressorInputStream.matches(signature, signatureLength)) {
                return new BZip2CompressorInputStream(in, decompressConcatenated);
            }

            if (GzipCompressorInputStream.matches(signature, signatureLength)) {
                return new GzipCompressorInputStream(in, decompressConcatenated);
            }

            if (Pack200CompressorInputStream.matches(signature, signatureLength)) {
                return new Pack200CompressorInputStream(in);
            }

            if (FramedSnappyCompressorInputStream.matches(signature, signatureLength)) {
                return new FramedSnappyCompressorInputStream(in);
            }

            if (ZCompressorInputStream.matches(signature, signatureLength)) {
                return new ZCompressorInputStream(in);
            }

            if (DeflateCompressorInputStream.matches(signature, signatureLength)) {
                return new DeflateCompressorInputStream(in);
            }

            if (XZUtils.matches(signature, signatureLength) && XZUtils.isXZCompressionAvailable()) {
                return new XZCompressorInputStream(in, decompressConcatenated);
            }

            if (LZMAUtils.matches(signature, signatureLength) && LZMAUtils.isLZMACompressionAvailable()) {
                return new LZMACompressorInputStream(in);
            }

        } catch (final IOException e) {
            throw new CompressorException("Failed to detect Compressor from InputStream.", e);
        }

        throw new CompressorException("No Compressor found for the stream signature.");
    }

    /**
     * Creates a compressor input stream from a compressor name and an input
     * stream.
     * 
     * @param name
     *            of the compressor, i.e. {@value #GZIP}, {@value #BZIP2},
     *            {@value #XZ}, {@value #LZMA}, {@value #PACK200},
     *            {@value #SNAPPY_RAW}, {@value #SNAPPY_FRAMED}, {@value #Z},
     *            {@value #LZ4_BLOCK}
     *            or {@value #DEFLATE}
     * @param in
     *            the input stream
     * @return compressor input stream
     * @throws CompressorException
     *             if the compressor name is not known
     * @throws IllegalArgumentException
     *             if the name or input stream is null
     */
    public CompressorInputStream createCompressorInputStream(final String name, final InputStream in)
            throws CompressorException {
        return createCompressorInputStream(name, in, decompressConcatenated);
    }

    @Override
    public CompressorInputStream createCompressorInputStream(final String name, final InputStream in,
            final boolean actualDecompressConcatenated) throws CompressorException {
        if (name == null || in == null) {
            throw new IllegalArgumentException("Compressor name and stream must not be null.");
        }

        try {

            if (GZIP.equalsIgnoreCase(name)) {
                return new GzipCompressorInputStream(in, actualDecompressConcatenated);
            }

            if (BZIP2.equalsIgnoreCase(name)) {
                return new BZip2CompressorInputStream(in, actualDecompressConcatenated);
            }

            if (XZ.equalsIgnoreCase(name)) {
                return new XZCompressorInputStream(in, actualDecompressConcatenated);
            }

            if (LZMA.equalsIgnoreCase(name)) {
                return new LZMACompressorInputStream(in);
            }

            if (PACK200.equalsIgnoreCase(name)) {
                return new Pack200CompressorInputStream(in);
            }

            if (SNAPPY_RAW.equalsIgnoreCase(name)) {
                return new SnappyCompressorInputStream(in);
            }

            if (SNAPPY_FRAMED.equalsIgnoreCase(name)) {
                return new FramedSnappyCompressorInputStream(in);
            }

            if (Z.equalsIgnoreCase(name)) {
                return new ZCompressorInputStream(in);
            }

            if (DEFLATE.equalsIgnoreCase(name)) {
                return new DeflateCompressorInputStream(in);
            }

            if (LZ4_BLOCK.equalsIgnoreCase(name)) {
                return new BlockLZ4CompressorInputStream(in);
            }

        } catch (final IOException e) {
            throw new CompressorException("Could not create CompressorInputStream.", e);
        }
        final CompressorStreamProvider compressorStreamProvider = getCompressorInputStreamProviders().get(toKey(name));
        if (compressorStreamProvider != null) {
            return compressorStreamProvider.createCompressorInputStream(name, in, actualDecompressConcatenated);
        }
        
        throw new CompressorException("Compressor: " + name + " not found.");
    }

    /**
     * Creates an compressor output stream from an compressor name and an output
     * stream.
     * 
     * @param name
     *            the compressor name, i.e. {@value #GZIP}, {@value #BZIP2},
     *            {@value #XZ}, {@value #PACK200}, {@value SNAPPY_FRAMED},
     *            {@value LZ4_BLOCK}
     *            or {@value #DEFLATE}
     * @param out
     *            the output stream
     * @return the compressor output stream
     * @throws CompressorException
     *             if the archiver name is not known
     * @throws IllegalArgumentException
     *             if the archiver name or stream is null
     */
    @Override
    public CompressorOutputStream createCompressorOutputStream(final String name, final OutputStream out)
            throws CompressorException {
        if (name == null || out == null) {
            throw new IllegalArgumentException("Compressor name and stream must not be null.");
        }

        try {

            if (GZIP.equalsIgnoreCase(name)) {
                return new GzipCompressorOutputStream(out);
            }

            if (BZIP2.equalsIgnoreCase(name)) {
                return new BZip2CompressorOutputStream(out);
            }

            if (XZ.equalsIgnoreCase(name)) {
                return new XZCompressorOutputStream(out);
            }

            if (PACK200.equalsIgnoreCase(name)) {
                return new Pack200CompressorOutputStream(out);
            }

            if (LZMA.equalsIgnoreCase(name)) {
                return new LZMACompressorOutputStream(out);
            }

            if (DEFLATE.equalsIgnoreCase(name)) {
                return new DeflateCompressorOutputStream(out);
            }

            if (SNAPPY_FRAMED.equalsIgnoreCase(name)) {
                return new FramedSnappyCompressorOutputStream(out);
            }

            if (LZ4_BLOCK.equalsIgnoreCase(name)) {
                return new BlockLZ4CompressorOutputStream(out);
            }

        } catch (final IOException e) {
            throw new CompressorException("Could not create CompressorOutputStream", e);
        }
        final CompressorStreamProvider compressorStreamProvider = getCompressorOutputStreamProviders().get(toKey(name));
        if (compressorStreamProvider != null) {
            return compressorStreamProvider.createCompressorOutputStream(name, out);
        }
        throw new CompressorException("Compressor: " + name + " not found.");
    }

    public SortedMap<String, CompressorStreamProvider> getCompressorInputStreamProviders() {
        if (compressorInputStreamProviders == null) {
            compressorInputStreamProviders = Collections
                    .unmodifiableSortedMap(findAvailableCompressorInputStreamProviders());
        }
        return compressorInputStreamProviders;
    }

    public SortedMap<String, CompressorStreamProvider> getCompressorOutputStreamProviders() {
        if (compressorOutputStreamProviders == null) {
            compressorOutputStreamProviders = Collections
                    .unmodifiableSortedMap(findAvailableCompressorOutputStreamProviders());
        }
        return compressorOutputStreamProviders;
    }

    // For Unit tests
    boolean getDecompressConcatenated() {
        return decompressConcatenated;
    }

    public Boolean getDecompressUntilEOF() {
        return decompressUntilEOF;
    }

    @Override
    public Set<String> getInputStreamCompressorNames() {
        return Sets.newHashSet(GZIP, BZIP2, XZ, LZMA, PACK200, SNAPPY_RAW, SNAPPY_FRAMED, Z, DEFLATE, LZ4_BLOCK);
    }

    @Override
    public Set<String> getOutputStreamCompressorNames() {
        return Sets.newHashSet(GZIP, BZIP2, XZ, LZMA, PACK200, DEFLATE, SNAPPY_FRAMED, LZ4_BLOCK);
    }

    /**
     * Whether to decompress the full input or only the first stream in formats
     * supporting multiple concatenated input streams.
     *
     * <p>
     * This setting applies to the gzip, bzip2 and xz formats only.
     * </p>
     *
     * @param decompressConcatenated
     *            if true, decompress until the end of the input; if false, stop
     *            after the first stream and leave the input position to point
     *            to the next byte after the stream
     * @since 1.5
     * @deprecated 1.10 use the {@link #CompressorStreamFactory(boolean)}
     *             constructor instead
     * @throws IllegalStateException
     *             if the constructor {@link #CompressorStreamFactory(boolean)}
     *             was used to create the factory
     */
    @Deprecated
    public void setDecompressConcatenated(final boolean decompressConcatenated) {
        if (this.decompressUntilEOF != null) {
            throw new IllegalStateException("Cannot override the setting defined by the constructor");
        }
        this.decompressConcatenated = decompressConcatenated;
    }
    
}
