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

package org.broad.igv.dev.plugin;

import org.apache.log4j.Logger;
import org.broad.tribble.AsciiFeatureCodec;
import org.broad.tribble.Feature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: jacob
 * Date: 2012-Sep-27
 */
public abstract class AsciiDecoder<T extends Feature> implements FeatureDecoder<T> {

    private static Logger log = Logger.getLogger(AsciiDecoder.class);

    /**
     * Decode all features from the specified input stream
     * Stream is closed afterwards
     *
     * @param is
     * @param strictParsing If true, errors are thrown if we cannot parse a given line.
     *                      If false, we simply skip that line
     * @return
     * @throws IOException
     */
    public Iterator<T> decodeAll(InputStream is, boolean strictParsing) throws IOException {

        List<T> featuresList = new ArrayList<T>();
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        String line;
        T feat;
        while ((line = bis.readLine()) != null) {

            feat = decode(line, strictParsing);

            if (feat != null) {
                featuresList.add(feat);
            }

        }

        is.close();
        return featuresList.iterator();
    }

    public abstract T decode(String line, boolean strictParsing);

    @Override
    public T decode(String line) {
        return decode(line, true);
    }

    @Override
    public void setOutputColumns(Map<String, Integer> outputColumns) {
    }

    @Override
    public void setInputs(List<String> commands, Map<Argument, Object> argumentMap) {
    }

    public static class DecoderWrapper<T extends Feature> extends AsciiDecoder<T> {

        private AsciiFeatureCodec<T> wrappedCodec;

        public DecoderWrapper(AsciiFeatureCodec<T> wrappedCodec) {
            this.wrappedCodec = wrappedCodec;
        }

        @Override
        public T decode(String line, boolean strictParsing) {
            T feat = null;
            try {
                feat = wrappedCodec.decode(line);
            } catch (Exception e) {
                log.error(e);
                if (strictParsing) {
                    throw new RuntimeException(e);
                }
            }

            return feat;
        }

    }
}
