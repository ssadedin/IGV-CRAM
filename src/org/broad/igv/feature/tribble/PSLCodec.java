/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.feature.tribble;

import org.broad.igv.Globals;
import org.broad.igv.feature.PSLRecord;
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.Exon;
import org.broad.igv.feature.Strand;
import org.broad.igv.feature.genome.Genome;

/**
 * @author jrobinso
 * @date Aug 5, 2010
 * <p/>
 * Columns:
 * <p/>
 * 1. matches - Number of bases that match that aren't repeats
 * 2. misMatches - Number of bases that don't match
 * 3. repMatches - Number of bases that match but are part of repeats
 * 4. nCount - Number of 'N' bases
 * 5. qNumInsert - Number of inserts in query
 * 6. qBaseInsert - Number of bases inserted in query
 * 7. tNumInsert - Number of inserts in target
 * 8. tBaseInsert - Number of bases inserted in target
 * 9. strand - '+' or '-' for query strand. For translated alignments, second '+'or '-' is for genomic strand
 * 10. qName - Query sequence name
 * 11. qSize - Query sequence size
 * 12. qStart - Alignment start position in query
 * 13. qEnd - Alignment end position in query
 * 14. tName - Target sequence name
 * 15. tSize - Target sequence size
 * 16. tStart - Alignment start position in target
 * 17. tEnd - Alignment end position in target
 * 18. blockCount - Number of blocks in the alignment (a block contains no gaps)
 * 19. blockSizes - Comma-separated list of sizes of each block
 * 20. qStarts - Comma-separated list of starting positions of each block in query
 * 21. tStarts - Comma-separated list of starting positions of each block in target
 */
public class PSLCodec extends UCSCCodec<BasicFeature> {

    Genome genome;
    boolean keepText;

    public PSLCodec(){
        this(null);
    }

    public PSLCodec(Genome genome) {
        this(genome, false);
    }

    public PSLCodec(Genome genome, boolean keepText) {
        super(BasicFeature.class);
        this.genome = genome;
        this.keepText = keepText;

    }

    public PSLRecord decode(String line) {

        PSLRecord f = null;
        try {
            if (line.trim().length() == 0 || line.startsWith("#") || line.startsWith("track") ||
                    line.startsWith("browser")) {
                return null;
            }

            String[] tokens = Globals.singleTabMultiSpacePattern.split(line);
            int nTokens = tokens.length;
            if (nTokens < 21) {
                // log.info("Skipping line ")
                return null;
            }
            int tSize = Integer.parseInt(tokens[14]);
            String chrToken = tokens[13];
            String chr = genome == null ? chrToken : genome.getChromosomeAlias(chrToken);
            int start = Integer.parseInt(tokens[15]); // IS PSL 1 or ZERO based,  closed or open?

            String strandString = tokens[8];
            Strand strand = strandString.startsWith("+") ? Strand.POSITIVE : Strand.NEGATIVE;

            boolean gNeg = false;
            if (strandString.length() > 1) {
                gNeg = strandString.charAt(1) == '-';
            }

            f = new PSLRecord();
            f.setName(tokens[9]);
            f.setChr(chr);
            f.setStart(start);
            f.setEnd(Integer.parseInt(tokens[16]));
            f.setStrand(strand);

            int exonCount = Integer.parseInt(tokens[17]);
            String[] exonSizes = tokens[18].split(",");
            String[] startsBuffer = tokens[20].split(",");

            if (startsBuffer.length == exonSizes.length && exonCount == startsBuffer.length) {
                for (int i = 0; i < startsBuffer.length; i++) {
                    int exonSize = Integer.parseInt(exonSizes[i]);
                    int exonStart = Integer.parseInt(startsBuffer[i]);
                    if (gNeg) {
                        exonStart = tSize - exonStart - exonSize;
                    }
                    int exonEnd = exonStart + exonSize;
                    Exon exon = new Exon(chr, exonStart, exonEnd, strand);
                    f.addExon(exon);
                }
            } else {
                // TODO -- warn
            }

            //score = percentId = 100.0 * (match + repMatch)  / (misMatch + match + repMatch + qGapCount + tGapCount)
            int match = Integer.parseInt(tokens[0]);
            int misMatch = Integer.parseInt(tokens[1]);
            int repMatch = Integer.parseInt(tokens[2]);
            int ns = Integer.parseInt(tokens[3]);
            int qGapCount = Integer.parseInt(tokens[4]);
            int qGapBases = Integer.parseInt(tokens[5]);
            int tGapCount = Integer.parseInt(tokens[6]);
            int tGapBases = Integer.parseInt(tokens[7]);
            int qSize = Integer.parseInt(tokens[10]);

            float score = (1000.0f * (match + repMatch - misMatch - qGapCount - tGapCount)) / qSize;

            f.setMatch(match);
            f.setMisMatch(misMatch);
            f.setRepMatch(repMatch);
            f.setNs(ns);
            f.setQGapCount(qGapCount);
            f.setQGapBases(qGapBases);
            f.setTGapCount(tGapCount);
            f.setTGapBases(tGapBases);
            f.setQSize(qSize);

            f.setScore(score);

            // Build description
            StringBuffer desc = new StringBuffer();
            desc.append("matches = " + tokens[0]);
            desc.append("<br>");
            desc.append("mismatches = " + tokens[1]);
            desc.append("<br>");
            desc.append("repeat matches = " + tokens[2]);
            desc.append("<br>");
            desc.append("# inserts in query = " + tokens[4]);
            desc.append("<br>");
            desc.append("# inserts in target = " + tokens[6]);
            f.setDescription(desc.toString());

            if(keepText) {
                f.setText(line);
            }

        } catch (NumberFormatException e) {
            return null;
        }


        return f;
    }

    /**
     * This function returns true iff the File potentialInput can be parsed by this
     * codec.
     * <p/>
     * There is an assumption that there's never a situation where two different Codecs
     * return true for the same file.  If this occurs, the recommendation would be to error out.
     * <p/>
     * Note this function must never throw an error.  All errors should be trapped
     * and false returned.
     *
     * @param path the file to test for parsability with this codec
     * @return true if potentialInput can be parsed, false otherwise
     */
    public boolean canDecode(String path) {
        return path.toLowerCase().endsWith(".psl");
    }
}
