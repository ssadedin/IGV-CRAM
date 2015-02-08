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

package org.broad.igv.cli_plugin;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import org.broad.igv.sam.PicardAlignment;
import org.broad.igv.sam.reader.WrappedIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author jacob
 * @since 2012-Oct-01
 */
public class AlignmentDecoder implements FeatureDecoder<PicardAlignment> {

    @Override
    public Iterator<PicardAlignment> decodeAll(InputStream is, boolean strictParsing) throws IOException {
        SAMFileReader reader = new SAMFileReader(is);
        ValidationStringency stringency =
                strictParsing ? ValidationStringency.STRICT : ValidationStringency.SILENT;
        reader.setValidationStringency(stringency);
        return new WrappedIterator(reader.iterator());
    }

    @Override
    public void setAttributes(List<Map<String, Object>> attributes) {
        //pass
    }

    @Override
    public void setInputs(List<String> commands, Map<Argument, Object> argumentMap) {
        //pass
    }
}
