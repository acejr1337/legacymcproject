// 
// Decompiled by Procyon v0.6-prerelease
// 

package LZMA;

import java.io.IOException;
import java.io.InputStream;

class CRangeDecoder
{
    static final int kNumTopBits = 24;
    static final int kTopValue = 16777216;
    static final int kTopValueMask = -16777216;
    static final int kNumBitModelTotalBits = 11;
    static final int kBitModelTotal = 2048;
    static final int kNumMoveBits = 5;
    InputStream inStream;
    int Range;
    int Code;
    byte[] buffer;
    int buffer_size;
    int buffer_ind;
    static final int kNumPosBitsMax = 4;
    static final int kNumPosStatesMax = 16;
    static final int kLenNumLowBits = 3;
    static final int kLenNumLowSymbols = 8;
    static final int kLenNumMidBits = 3;
    static final int kLenNumMidSymbols = 8;
    static final int kLenNumHighBits = 8;
    static final int kLenNumHighSymbols = 256;
    static final int LenChoice = 0;
    static final int LenChoice2 = 1;
    static final int LenLow = 2;
    static final int LenMid = 130;
    static final int LenHigh = 258;
    static final int kNumLenProbs = 514;
    
    CRangeDecoder(final InputStream inStream) throws IOException {
        this.buffer = new byte[16384];
        this.inStream = inStream;
        this.Code = 0;
        this.Range = -1;
        for (int i = 0; i < 5; ++i) {
            this.Code = (this.Code << 8 | this.Readbyte());
        }
    }
    
    int Readbyte() throws IOException {
        if (this.buffer_size == this.buffer_ind) {
            this.buffer_size = this.inStream.read(this.buffer);
            this.buffer_ind = 0;
            if (this.buffer_size < 1) {
                throw new LzmaException("LZMA : Data Error");
            }
        }
        return this.buffer[this.buffer_ind++] & 0xFF;
    }
    
    int DecodeDirectBits(final int n) throws IOException {
        int n2 = 0;
        for (int i = n; i > 0; --i) {
            this.Range >>>= 1;
            final int n3 = this.Code - this.Range >>> 31;
            this.Code -= (this.Range & n3 - 1);
            n2 = (n2 << 1 | 1 - n3);
            if (this.Range < 16777216) {
                this.Code = (this.Code << 8 | this.Readbyte());
                this.Range <<= 8;
            }
        }
        return n2;
    }
    
    int BitDecode(final int[] array, final int n) throws IOException {
        final int range = (this.Range >>> 11) * array[n];
        if (((long)this.Code & 0xFFFFFFFFL) < ((long)range & 0xFFFFFFFFL)) {
            this.Range = range;
            array[n] += 2048 - array[n] >>> 5;
            if ((this.Range & 0xFF000000) == 0x0) {
                this.Code = (this.Code << 8 | this.Readbyte());
                this.Range <<= 8;
            }
            return 0;
        }
        this.Range -= range;
        this.Code -= range;
        array[n] -= array[n] >>> 5;
        if ((this.Range & 0xFF000000) == 0x0) {
            this.Code = (this.Code << 8 | this.Readbyte());
            this.Range <<= 8;
        }
        return 1;
    }
    
    int BitTreeDecode(final int[] array, final int n, final int n2) throws IOException {
        int n3 = 1;
        for (int i = n2; i > 0; --i) {
            n3 = n3 + n3 + this.BitDecode(array, n + n3);
        }
        return n3 - (1 << n2);
    }
    
    int ReverseBitTreeDecode(final int[] array, final int n, final int n2) throws IOException {
        int n3 = 1;
        int n4 = 0;
        for (int i = 0; i < n2; ++i) {
            final int bitDecode = this.BitDecode(array, n + n3);
            n3 = n3 + n3 + bitDecode;
            n4 |= bitDecode << i;
        }
        return n4;
    }
    
    byte LzmaLiteralDecode(final int[] array, final int n) throws IOException {
        int i = 1;
        do {
            i = (i + i | this.BitDecode(array, n + i));
        } while (i < 256);
        return (byte)i;
    }
    
    byte LzmaLiteralDecodeMatch(final int[] array, final int n, byte b) throws IOException {
        int i = 1;
        do {
            final int n2 = b >> 7 & 0x1;
            b <<= 1;
            final int bitDecode = this.BitDecode(array, n + (1 + n2 << 8) + i);
            i = (i << 1 | bitDecode);
            if (n2 != bitDecode) {
                while (i < 256) {
                    i = (i + i | this.BitDecode(array, n + i));
                }
                break;
            }
        } while (i < 256);
        return (byte)i;
    }
    
    int LzmaLenDecode(final int[] array, final int n, final int n2) throws IOException {
        if (this.BitDecode(array, n + 0) == 0) {
            return this.BitTreeDecode(array, n + 2 + (n2 << 3), 3);
        }
        if (this.BitDecode(array, n + 1) == 0) {
            return 8 + this.BitTreeDecode(array, n + 130 + (n2 << 3), 3);
        }
        return 16 + this.BitTreeDecode(array, n + 258, 8);
    }
}
