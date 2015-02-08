/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.maf;

import net.sf.samtools.seekablestream.SeekableStream;
import org.broad.igv.Globals;
import org.broad.igv.util.ParsingUtils;
import org.broad.igv.util.index.Interval;
import org.broad.igv.util.index.IntervalTree;
import org.broad.igv.util.stream.IGVSeekableStreamFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jrobinso
 *         Date: 2/18/13
 *         Time: 9:59 AM
 */
public class MAFParser implements MAFReader {

    String path;
    MAFIndex index;
    List<String> species;
    String trackName;

    public MAFParser(String path) {
        this.path = path;
        try {
            parseHeader();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        String indexPath = path + ".index";
        try {
            if (ParsingUtils.pathExists(indexPath)) {
                index = MAFIndex.loadIndex(indexPath);
            } else {
                index = MAFIndex.createIndex(path);
                MAFIndex.writeIndex(index, indexPath);
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    public String getTrackName() {
        return trackName;
    }

    @Override
    public List<MultipleAlignmentBlock> loadAlignments(String chr, int start, int end) throws IOException {

        IntervalTree ivTree = index.getIntervalTree(chr);
        if (ivTree == null) return null;

        List<Interval> intervals = ivTree.findOverlapping(start, end);
        if (intervals.isEmpty()) {
            return null;
        }


        // Find the starting (left most) interval.  Alignment blocks do not overlap, so we can start at the
        // minimum file offset and just proceed until the end of the interval.
        long startPosition = Long.MAX_VALUE;
        for (Interval iv : intervals) {
            startPosition = Math.min(startPosition, iv.getValue());
        }


        SeekableStream is = null;


        is = IGVSeekableStreamFactory.getInstance().getStreamFor(path);
        is.seek(startPosition);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 256000);

        List<MultipleAlignmentBlock> alignments = new ArrayList<MultipleAlignmentBlock>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("a ")) {
                // TODO -- parse score (optional)
                MultipleAlignmentBlock block = parseBlock(reader);
                if(block.getEnd() < start) {
                    continue;
                }
                if (block.getStart() > end || !block.getChr().equals(chr)) {
                    break;
                } else {
                    alignments.add(block);
                }
            }
        }
        return alignments;
    }


    @Override
    public Collection<String> getChrNames() {
        return index.getChromosomes();
    }

    @Override
    public Collection<String> getSpecies() {
        return species != null ? species : index.getSpecies();
    }

    @Override
    public String getSpeciesName(String speciesId) {
        return speciesId;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRefId() {
        return index.getRefId();  //To change body of implemented methods use File | Settings | File Templates.
    }


    void parseHeader() throws IOException {

        BufferedReader reader = null;

        try {
            InputStream is = IGVSeekableStreamFactory.getInstance().getStreamFor(path);
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.startsWith("a ")) {
                    return;  // Done with header
                } else if (line.startsWith("track")) {

                    String[] tokens = breakQuotedString(line);
                    for (int i = 0; i < tokens.length; i++) {
                        String key = null;
                        String value = null;
                        String[] kv = tokens[i].split("=");
                        if (kv.length == 1) {
                            if (tokens[i].endsWith(("="))) {
                                key = kv[0].toLowerCase().trim();
                                value = tokens[++i];
                            }
                        } else if (kv.length == 2) {
                            key = kv[0].toLowerCase().trim();
                            value = kv[1];
                        }
                        if (key != null) {
                            if (key.equals("name")) {
                                this.trackName = value;
                            }
                            if (key.equals("speciesorder")) {
                                species = Arrays.asList(Globals.whitespacePattern.split(value));
                            }
                        }
                    }
                }
            }
        } finally {
            if (reader != null) reader.close();
        }
    }

    private String[] breakQuotedString(String subjectString) {

        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(subjectString);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }

        return matchList.toArray(new String[]{});
    }


    /**
     * Parse an alignment block.
     *
     * @param reader
     */
    private MultipleAlignmentBlock parseBlock(BufferedReader reader) throws IOException {

        String line;
        MultipleAlignmentBlock ma = new MultipleAlignmentBlock();

        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                return ma;
            }
            if (line.startsWith("s ")) {

                String[] tokens = Globals.whitespacePattern.split(line);

                String src = tokens[1];
                String species = src;
                String chr = src;
                if (src.contains(".")) {
                    String[] srcTokens = ParsingUtils.PERIOD_PATTERN.split(src);
                    species = srcTokens[0];
                    chr = srcTokens[1];
                }
                int start = Integer.parseInt(tokens[2]);
                int size = Integer.parseInt(tokens[3]);
                char strand = tokens[4].charAt(0);
                int srcSize = Integer.parseInt(tokens[5]);
                String text = tokens[6];

                ma.addSequence(new MultipleAlignmentBlock.Sequence(species, chr, start, size, strand, srcSize, text));


            }


        }
        return ma;
    }

}
