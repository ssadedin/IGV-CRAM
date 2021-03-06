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

package org.broad.igv.feature;

import org.broad.igv.AbstractHeadlessTest;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.MutationTrack;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.TestUtils;
import htsjdk.tribble.Feature;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * User: jacob
 * Date: 2012-Oct-23
 */
public class MutationTrackLoaderTest extends AbstractHeadlessTest {
    @Test
    public void testLoadMutationTrackGZ() throws Exception {
        String testPath = TestUtils.DATA_DIR + "maf/TCGA_GBM_Level3_Somatic_Mutations_08.28.2008.maf.gz";
        MutationTrackLoader parser = new MutationTrackLoader();
        List<FeatureTrack> trackList = parser.loadMutationTracks(new ResourceLocator(testPath), genome);

        String testSampleId = "TCGA-02-0024-01B-01W";
        String testChr = "chr12";
        int testStart = 9150104 - 1;
        int testEnd = testStart + 1;

        //We only look at one line, line 4 of the file:
        //A2M	2	genome.wustl.edu	36	12	9150104	9150104	+	Missense_Mutation	SNP	C	C	G	Unknown	Unknown	TCGA-02-0024-01B-01W	TCGA-02-0024-10A-01W	C	C	C	G	C	C	Valid	Valid	Somatic	Phase_I

        //There should be 1 track per sampleId
        Set<String> sampleIds = new HashSet<String>(trackList.size());
        for (FeatureTrack track : trackList) {
            MutationTrack mutTrack = (MutationTrack) track;

            assertFalse(sampleIds.contains(mutTrack.getName()));

            sampleIds.add(mutTrack.getName());
            if (mutTrack.getName().equals(testSampleId)) {
                List<Feature> features = mutTrack.getFeatures("chr12", testStart, testEnd);
                assertEquals(1, features.size());
                Mutation mut = (Mutation) features.get(0);

                assertEquals(testSampleId, mut.getSampleId());
                assertEquals(testChr, mut.getChr());
                assertEquals(testStart, mut.getStart());
                assertEquals(testEnd, mut.getEnd());
                assertEquals("Missense_Mutation", mut.getMutationType());
                assertEquals("CCG", mut.refAllele + mut.altAllele1 + mut.altAllele2);
            }
        }

        assertEquals(sampleIds.size(), trackList.size());

    }
}
