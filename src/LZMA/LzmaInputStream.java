// 
// Decompiled by Procyon v0.6-prerelease
// 

package LZMA;

import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;

public class LzmaInputStream extends FilterInputStream
{
    boolean isClosed;
    CRangeDecoder RangeDecoder;
    byte[] dictionary;
    int dictionarySize;
    int dictionaryPos;
    int GlobalPos;
    int rep0;
    int rep1;
    int rep2;
    int rep3;
    int lc;
    int lp;
    int pb;
    int State;
    boolean PreviousIsMatch;
    int RemainLen;
    int[] probs;
    byte[] uncompressed_buffer;
    int uncompressed_size;
    int uncompressed_offset;
    long GlobalNowPos;
    long GlobalOutSize;
    static final int LZMA_BASE_SIZE = 1846;
    static final int LZMA_LIT_SIZE = 768;
    static final int kBlockSize = 65536;
    static final int kNumStates = 12;
    static final int kStartPosModelIndex = 4;
    static final int kEndPosModelIndex = 14;
    static final int kNumFullDistances = 128;
    static final int kNumPosSlotBits = 6;
    static final int kNumLenToPosStates = 4;
    static final int kNumAlignBits = 4;
    static final int kAlignTableSize = 16;
    static final int kMatchMinLen = 2;
    static final int IsMatch = 0;
    static final int IsRep = 192;
    static final int IsRepG0 = 204;
    static final int IsRepG1 = 216;
    static final int IsRepG2 = 228;
    static final int IsRep0Long = 240;
    static final int PosSlot = 432;
    static final int SpecPos = 688;
    static final int Align = 802;
    static final int LenCoder = 818;
    static final int RepLenCoder = 1332;
    static final int Literal = 1846;
    
    public LzmaInputStream(final InputStream inputStream) throws IOException {
        super(inputStream);
        this.isClosed = false;
        this.readHeader();
        this.fill_buffer();
    }
    
    private void LzmaDecode(final int n) throws IOException {
        final int n2 = (1 << this.pb) - 1;
        final int n3 = (1 << this.lp) - 1;
        this.uncompressed_size = 0;
        if (this.RemainLen == -1) {
            return;
        }
        while (this.RemainLen > 0 && this.uncompressed_size < n) {
            int n4 = this.dictionaryPos - this.rep0;
            if (n4 < 0) {
                n4 += this.dictionarySize;
            }
            this.uncompressed_buffer[this.uncompressed_size++] = (this.dictionary[this.dictionaryPos] = this.dictionary[n4]);
            if (++this.dictionaryPos == this.dictionarySize) {
                this.dictionaryPos = 0;
            }
            --this.RemainLen;
        }
        byte b;
        if (this.dictionaryPos == 0) {
            b = this.dictionary[this.dictionarySize - 1];
        }
        else {
            b = this.dictionary[this.dictionaryPos - 1];
        }
        while (this.uncompressed_size < n) {
            final int n5 = this.uncompressed_size + this.GlobalPos & n2;
            if (this.RangeDecoder.BitDecode(this.probs, 0 + (this.State << 4) + n5) == 0) {
                final int n6 = 1846 + 768 * (((this.uncompressed_size + this.GlobalPos & n3) << this.lc) + ((b & 0xFF) >> 8 - this.lc));
                if (this.State < 4) {
                    this.State = 0;
                }
                else if (this.State < 10) {
                    this.State -= 3;
                }
                else {
                    this.State -= 6;
                }
                if (this.PreviousIsMatch) {
                    int n7 = this.dictionaryPos - this.rep0;
                    if (n7 < 0) {
                        n7 += this.dictionarySize;
                    }
                    b = this.RangeDecoder.LzmaLiteralDecodeMatch(this.probs, n6, this.dictionary[n7]);
                    this.PreviousIsMatch = false;
                }
                else {
                    b = this.RangeDecoder.LzmaLiteralDecode(this.probs, n6);
                }
                this.uncompressed_buffer[this.uncompressed_size++] = b;
                this.dictionary[this.dictionaryPos] = b;
                if (++this.dictionaryPos != this.dictionarySize) {
                    continue;
                }
                this.dictionaryPos = 0;
            }
            else {
                this.PreviousIsMatch = true;
                if (this.RangeDecoder.BitDecode(this.probs, 192 + this.State) == 1) {
                    if (this.RangeDecoder.BitDecode(this.probs, 204 + this.State) == 0) {
                        if (this.RangeDecoder.BitDecode(this.probs, 240 + (this.State << 4) + n5) == 0) {
                            if (this.uncompressed_size + this.GlobalPos == 0) {
                                throw new LzmaException("LZMA : Data Error");
                            }
                            this.State = ((this.State < 7) ? 9 : 11);
                            int n8 = this.dictionaryPos - this.rep0;
                            if (n8 < 0) {
                                n8 += this.dictionarySize;
                            }
                            b = this.dictionary[n8];
                            this.dictionary[this.dictionaryPos] = b;
                            if (++this.dictionaryPos == this.dictionarySize) {
                                this.dictionaryPos = 0;
                            }
                            this.uncompressed_buffer[this.uncompressed_size++] = b;
                            continue;
                        }
                    }
                    else {
                        int rep0;
                        if (this.RangeDecoder.BitDecode(this.probs, 216 + this.State) == 0) {
                            rep0 = this.rep1;
                        }
                        else {
                            if (this.RangeDecoder.BitDecode(this.probs, 228 + this.State) == 0) {
                                rep0 = this.rep2;
                            }
                            else {
                                rep0 = this.rep3;
                                this.rep3 = this.rep2;
                            }
                            this.rep2 = this.rep1;
                        }
                        this.rep1 = this.rep0;
                        this.rep0 = rep0;
                    }
                    this.RemainLen = this.RangeDecoder.LzmaLenDecode(this.probs, 1332, n5);
                    this.State = ((this.State < 7) ? 8 : 11);
                }
                else {
                    this.rep3 = this.rep2;
                    this.rep2 = this.rep1;
                    this.rep1 = this.rep0;
                    this.State = ((this.State < 7) ? 7 : 10);
                    this.RemainLen = this.RangeDecoder.LzmaLenDecode(this.probs, 818, n5);
                    final int bitTreeDecode = this.RangeDecoder.BitTreeDecode(this.probs, 432 + (((this.RemainLen < 4) ? this.RemainLen : 3) << 6), 6);
                    if (bitTreeDecode >= 4) {
                        final int n9 = (bitTreeDecode >> 1) - 1;
                        this.rep0 = (0x2 | (bitTreeDecode & 0x1)) << n9;
                        if (bitTreeDecode < 14) {
                            this.rep0 += this.RangeDecoder.ReverseBitTreeDecode(this.probs, 688 + this.rep0 - bitTreeDecode - 1, n9);
                        }
                        else {
                            this.rep0 += this.RangeDecoder.DecodeDirectBits(n9 - 4) << 4;
                            this.rep0 += this.RangeDecoder.ReverseBitTreeDecode(this.probs, 802, 4);
                        }
                    }
                    else {
                        this.rep0 = bitTreeDecode;
                    }
                    ++this.rep0;
                }
                if (this.rep0 == 0) {
                    this.RemainLen = -1;
                    break;
                }
                if (this.rep0 > this.uncompressed_size + this.GlobalPos) {
                    throw new LzmaException("LZMA : Data Error");
                }
                this.RemainLen += 2;
                do {
                    int n10 = this.dictionaryPos - this.rep0;
                    if (n10 < 0) {
                        n10 += this.dictionarySize;
                    }
                    b = this.dictionary[n10];
                    this.dictionary[this.dictionaryPos] = b;
                    if (++this.dictionaryPos == this.dictionarySize) {
                        this.dictionaryPos = 0;
                    }
                    this.uncompressed_buffer[this.uncompressed_size++] = b;
                    --this.RemainLen;
                } while (this.RemainLen > 0 && this.uncompressed_size < n);
            }
        }
        this.GlobalPos += this.uncompressed_size;
    }
    
    private void fill_buffer() throws IOException {
        if (this.GlobalNowPos < this.GlobalOutSize) {
            this.uncompressed_offset = 0;
            final long n = this.GlobalOutSize - this.GlobalNowPos;
            int n2;
            if (n > 65536L) {
                n2 = 65536;
            }
            else {
                n2 = (int)n;
            }
            this.LzmaDecode(n2);
            if (this.uncompressed_size == 0) {
                this.GlobalOutSize = this.GlobalNowPos;
            }
            else {
                this.GlobalNowPos += this.uncompressed_size;
            }
        }
    }
    
    private void readHeader() throws IOException {
        final byte[] array = new byte[5];
        if (5 != this.in.read(array)) {
            throw new LzmaException("LZMA header corrupted : Properties error");
        }
        this.GlobalOutSize = 0L;
        for (int i = 0; i < 8; ++i) {
            final int read = this.in.read();
            if (read == -1) {
                throw new LzmaException("LZMA header corrupted : Size error");
            }
            this.GlobalOutSize += (long)read << i * 8;
        }
        if (this.GlobalOutSize == -1L) {
            this.GlobalOutSize = Long.MAX_VALUE;
        }
        int j = array[0] & 0xFF;
        if (j >= 225) {
            throw new LzmaException("LZMA header corrupted : Properties error");
        }
        this.pb = 0;
        while (j >= 45) {
            ++this.pb;
            j -= 45;
        }
        this.lp = 0;
        while (j >= 9) {
            ++this.lp;
            j -= 9;
        }
        this.lc = j;
        this.probs = new int[1846 + (768 << this.lc + this.lp)];
        this.dictionarySize = 0;
        for (int k = 0; k < 4; ++k) {
            this.dictionarySize += (array[1 + k] & 0xFF) << k * 8;
        }
        this.dictionary = new byte[this.dictionarySize];
        if (this.dictionary == null) {
            throw new LzmaException("LZMA : can't allocate");
        }
        final int n = 1846 + (768 << this.lc + this.lp);
        this.RangeDecoder = new CRangeDecoder(this.in);
        this.dictionaryPos = 0;
        this.GlobalPos = 0;
        final int n2 = 1;
        this.rep3 = n2;
        this.rep2 = n2;
        this.rep1 = n2;
        this.rep0 = n2;
        this.State = 0;
        this.PreviousIsMatch = false;
        this.RemainLen = 0;
        this.dictionary[this.dictionarySize - 1] = 0;
        for (int l = 0; l < n; ++l) {
            this.probs[l] = 1024;
        }
        this.uncompressed_buffer = new byte[65536];
        this.uncompressed_size = 0;
        this.uncompressed_offset = 0;
        this.GlobalNowPos = 0L;
    }
    
    public int read(final byte[] array, final int n, final int n2) throws IOException {
        if (this.isClosed) {
            throw new IOException("stream closed");
        }
        if ((n | n2 | n + n2 | array.length - (n + n2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (n2 == 0) {
            return 0;
        }
        if (this.uncompressed_offset == this.uncompressed_size) {
            this.fill_buffer();
        }
        if (this.uncompressed_offset == this.uncompressed_size) {
            return -1;
        }
        final int min = Math.min(n2, this.uncompressed_size - this.uncompressed_offset);
        System.arraycopy(this.uncompressed_buffer, this.uncompressed_offset, array, n, min);
        this.uncompressed_offset += min;
        return min;
    }
    
    public void close() throws IOException {
        this.isClosed = true;
        super.close();
    }
}
