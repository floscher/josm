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
package org.apache.commons.compress.compressors.lz77support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Encapsulates code common to LZ77 decompressors.
 *
 * <p>Assumes the stream consists of blocks of literal data and
 * back-references (called copies) in any order. Of course the first
 * block must be a literal block for the scheme to work.</p>
 *
 * @since 1.14
 */
public abstract class AbstractLZ77CompressorInputStream extends CompressorInputStream {

    /** Size of the window - must be bigger than the biggest offset expected. */
    private final int windowSize;

    /** Buffer to write decompressed bytes to for back-references */
    private final byte[] buf;

    /** One behind the index of the last byte in the buffer that was written */
    private int writeIndex;

    /** Index of the next byte to be read. */
    private int readIndex;

    /** The underlying stream to read compressed data from */
    private final InputStream in;

    /** Number of bytes still to be read from the current literal or back-reference. */
    private long bytesRemaining;

    /** Offset of the current back-reference. */
    private int backReferenceOffset;

    /** uncompressed size */
    private int size = 0;

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];

    /**
     * Supplier that delegates to {@link #readOneByte}.
     */
    protected final ByteUtils.ByteSupplier supplier = new ByteUtils.ByteSupplier() {
        @Override
        public int getAsByte() throws IOException {
            return readOneByte();
        }
    };

    /**
     * Creates a new LZ77 input stream.
     *
     * @param is
     *            An InputStream to read compressed data from
     * @param windowSize
     *            Size of the window kept for back-references, must be bigger than the biggest offset expected.
     *
     * @throws IOException if reading fails
     */
    public AbstractLZ77CompressorInputStream(final InputStream is, int windowSize) throws IOException {
        this.in = is;
        this.windowSize = windowSize;
        buf = new byte[3 * windowSize];
        writeIndex = readIndex = 0;
        bytesRemaining = 0;
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        in.close();
    }

    /** {@inheritDoc} */
    @Override
    public int available() {
        return writeIndex - readIndex;
    }

    /**
     * Get the uncompressed size of the stream
     *
     * @return the uncompressed size
     */
    public int getSize() {
        return size;
    }

    /**
     * Used by subclasses to signal the next block contains the given
     * amount of literal data.
     * @param length the length of the block
     */
    protected final void startLiteral(long length) {
        bytesRemaining = length;
    }

    /**
     * Is there still data remaining inside the current block?
     * @return true if there is still data remaining inside the current block.
     */
    protected final boolean hasMoreDataInBlock() {
        return bytesRemaining > 0;
    }

    /**
     * Reads data from the current literal block.
     * @param b buffer to write data to
     * @param off offset to start writing to
     * @param len maximum amount of data to read
     * @return number of bytes read, may be 0. Will never return -1 as
     * EOF-detection is the responsibility of the subclass
     * @throws IOException if the underlying stream throws or signals
     * an EOF before the amount of data promised for the block have
     * been read
     */
    protected final int readLiteral(final byte[] b, final int off, final int len) throws IOException {
        final int avail = available();
        if (len > avail) {
            tryToReadLiteral(len - avail);
        }
        return readFromBuffer(b, off, len);
    }

    private void tryToReadLiteral(int bytesToRead) throws IOException {
        final int reallyTryToRead = (int) Math.min(Math.min(bytesToRead, bytesRemaining),
                                                   buf.length - writeIndex);
        final int bytesRead = reallyTryToRead > 0
            ? IOUtils.readFully(in, buf, writeIndex, reallyTryToRead)
            : 0 /* happens for bytesRemaining == 0 */;
        count(bytesRead);
        if (reallyTryToRead != bytesRead) {
            throw new IOException("Premature end of stream reading literal");
        }
        writeIndex += reallyTryToRead;
        bytesRemaining -= reallyTryToRead;
    }

    private int readFromBuffer(final byte[] b, final int off, final int len) {
        final int readable = Math.min(len, available());
        if (readable > 0) {
            System.arraycopy(buf, readIndex, b, off, readable);
            readIndex += readable;
            if (readIndex > 2 * windowSize) {
                slideBuffer();
            }
        }
        size += readable;
        return readable;
    }

    private void slideBuffer() {
        System.arraycopy(buf, windowSize, buf, 0, windowSize * 2);
        writeIndex -= windowSize;
        readIndex -= windowSize;
    }

    /**
     * Used by subclasses to signal the next block contains a back-reference with the given coordinates.
     * @param the offset of the back-reference
     * @param length the length of the back-reference
     */
    protected final void startBackReference(int offset, long length) {
        backReferenceOffset = offset;
        bytesRemaining = length;
    }

    /**
     * Reads data from the current back-reference.
     * @param b buffer to write data to
     * @param off offset to start writing to
     * @param len maximum amount of data to read
     * @return number of bytes read, may be 0. Will never return -1 as
     * EOF-detection is the responsibility of the subclass
     */
    protected final int readBackReference(final byte[] b, final int off, final int len) {
        final int avail = available();
        if (len > avail) {
            tryToCopy(len - avail);
        }
        return readFromBuffer(b, off, len);
    }

    private void tryToCopy(int bytesToCopy) {
        // this will fit into the buffer without sliding and not
        // require more than is available inside the back-reference
        int copy = (int) Math.min(Math.min(bytesToCopy, bytesRemaining),
                                  buf.length - writeIndex);
        if (copy == 0) {
            // NOP
        } else if (backReferenceOffset == 1) { // pretty common special case
            final byte last = buf[writeIndex - 1];
            Arrays.fill(buf, writeIndex, writeIndex + copy, last);
            writeIndex += copy;
        } else if (copy < backReferenceOffset) {
            System.arraycopy(buf, writeIndex - backReferenceOffset, buf, writeIndex, copy);
            writeIndex += copy;
        } else {
            final int fullRots = copy / backReferenceOffset;
            for (int i = 0; i < fullRots; i++) {
                System.arraycopy(buf, writeIndex - backReferenceOffset, buf, writeIndex, backReferenceOffset);
                writeIndex += backReferenceOffset;
            }

            final int pad = copy - (backReferenceOffset * fullRots);
            if (pad > 0) {
                System.arraycopy(buf, writeIndex - backReferenceOffset, buf, writeIndex, pad);
                writeIndex += pad;
            }
        }
        bytesRemaining -= copy;
    }

    /**
     * Reads a single byte from the real input stream and ensures the data is accounted for.
     *
     * @return the byte read as value between 0 and 255 or -1 if EOF has been reached.
     * @throws IOException if the underlying stream throws
     */
    protected final int readOneByte() throws IOException {
        final int b = in.read();
        if (b != -1) {
            count(1);
            return b & 0xFF;
        }
        return -1;
    }
}
