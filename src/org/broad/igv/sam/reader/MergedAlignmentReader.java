/*
 * Copyright (c) 2007-2013 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.sam.reader;

import htsjdk.samtools.SamFileHeaderMerger;
//import htsjdk.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.util.CloseableIterator;
import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.sam.Alignment;

import java.io.IOException;
import java.util.*;


/**
 * Performs a logical merge of bam files.
 * <p/>
 * User: jrobinso
 * Date: Apr 25, 2010
 */
public class MergedAlignmentReader implements AlignmentReader {

    private static Logger log = Logger.getLogger(MergedAlignmentReader.class);

    List<AlignmentReader> readers;
    List<String> sequenceNames;
    Map<String, Integer> chrNameIndex;
    SAMFileHeader header;

    public MergedAlignmentReader(List<AlignmentReader> readers) {
        this.readers = readers;
        loadSequenceNames();
    }

    public CloseableIterator<Alignment> iterator() {
        return new MergedFileIterator();
    }

    public CloseableIterator<Alignment> query(String chr, int start, int end, boolean contained) throws IOException {
        return new MergedFileIterator(chr, start, end, contained);
    }

    public void close() throws IOException {
        for (AlignmentReader reader : readers) {
            reader.close();
        }
    }

    public List<String> getSequenceNames() {
        return sequenceNames;
    }

    public Set<String> getPlatforms() {
        Set<String> platforms = new HashSet<String>();
        for (AlignmentReader reader : readers) {
            Set<String> plf = reader.getPlatforms();
            if(plf != null){
                platforms.addAll(plf);
            }
        }
        return platforms;
    }

    public SAMFileHeader getFileHeader(){
        if(this.header == null){
            this.header = loadHeaders();
        }
        return this.header;
    }

    /**
     * Return the merged list of all sequence names, maintaining order.
     *
     * @return
     */
    public void loadSequenceNames() {
        // Use a set for quick comparison
        LinkedHashSet<String> names = new LinkedHashSet<String>(50);
        for (AlignmentReader reader : readers) {
            names.addAll(reader.getSequenceNames());
        }
        sequenceNames = new ArrayList<String>(names);

        Genome genome = GenomeManager.getInstance().getCurrentGenome();
        chrNameIndex = new HashMap<String, Integer>(sequenceNames.size());
        for (int i = 0; i < sequenceNames.size(); i++) {
            final String seqName = sequenceNames.get(i);
            String chr = genome == null ? seqName : genome.getChromosomeAlias(seqName);
            chrNameIndex.put(chr, i);
        }
    }

    private SAMFileHeader loadHeaders(){
        List<SAMFileHeader> headersList = new ArrayList<SAMFileHeader>();
        SAMFileHeader.SortOrder sortOrder = null;
        for(AlignmentReader reader: readers){
            SAMFileHeader curHeader = reader.getFileHeader();
            if(curHeader != null) {
                headersList.add(curHeader);
                sortOrder = curHeader.getSortOrder();
            }
        }
        
        /*
        if(sortOrder != null){
            SamFileHeaderMerger headerMerger = new SamFileHeaderMerger(sortOrder, headersList, true);
            return headerMerger.getMergedHeader();
        }
        */

        return null;
    }
    
    public boolean hasIndex() {
        return readers.iterator().next().hasIndex();
    }


    public class MergedFileIterator implements CloseableIterator<Alignment> {

        List<CloseableIterator<Alignment>> allIterators = new ArrayList();
        PriorityQueue<RecordIterWrapper> iteratorQueue;

        public MergedFileIterator() {
            try {
                create(null, -1, -1, false);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        public MergedFileIterator(String chr, int start, int end, boolean contained) throws IOException {
            create(chr, start, end, contained);
        }

        private void create(String chr, int start, int end, boolean contained) throws IOException {
            iteratorQueue = new PriorityQueue(readers.size(), new AlignmentStartComparator());
            boolean iterate = (start == end) && (start == -1);
            for (AlignmentReader reader : readers) {
                CloseableIterator<Alignment> iter;
                if (iterate) {
                    iter = reader.iterator();
                } else {
                    iter = reader.query(chr, start, end, contained);
                }
                allIterators.add(iter);
                if (iter.hasNext()) {
                    iteratorQueue.add(new RecordIterWrapper(iter));
                }
            }
        }

        public boolean hasNext() {
            return iteratorQueue.size() > 0;
        }

        public Alignment next() {
            RecordIterWrapper wrapper = iteratorQueue.poll();
            Alignment next = wrapper.advance();
            if (wrapper.hasNext()) {
                iteratorQueue.add(wrapper);
            }
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove not implemented");
        }

        public void close() {
            for (CloseableIterator<Alignment> iter : allIterators) {
                iter.close();
            }
            allIterators.clear();
            iteratorQueue.clear();
        }

        class RecordIterWrapper {

            Alignment nextRecord;
            CloseableIterator<Alignment> iterator;

            RecordIterWrapper(CloseableIterator<Alignment> iter) {
                this.iterator = iter;
                nextRecord = (iterator.hasNext() ? iterator.next() : null);
            }

            Alignment advance() {
                Alignment tmp = nextRecord;
                nextRecord = (iterator.hasNext() ? iterator.next() : null);
                return tmp;

            }

            boolean hasNext() {
                return nextRecord != null;
            }

            void close() {
                if (iterator != null) {
                    System.out.println("Closing " + this);
                    iterator.close();
                    iterator = null;
                }
            }
        }

        class AlignmentStartComparator implements Comparator<RecordIterWrapper> {

            public int compare(RecordIterWrapper wrapper1, RecordIterWrapper wrapper2) {
                Alignment a1 = wrapper1.nextRecord;
                Alignment a2 = wrapper2.nextRecord;

                Integer idx1 = chrNameIndex.get(a1.getChr());
                Integer idx2 = chrNameIndex.get(a2.getChr());
                if (idx1 > idx2) {
                    return 1;
                } else if (idx1 < idx2) {
                    return -1;
                } else {
                    return a1.getAlignmentStart() - a2.getAlignmentStart();
                }
            }
        }
    }
}
