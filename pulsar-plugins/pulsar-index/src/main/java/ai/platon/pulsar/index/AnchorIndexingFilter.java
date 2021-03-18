/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.index;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.index.IndexingFilter;
import ai.platon.pulsar.persist.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map.Entry;

/**
 * Indexing filter that offers an option to either index all inbound anchor text
 * for a document or deduplicate anchors. Deduplication does have it's con's,
 */
public class AnchorIndexingFilter implements IndexingFilter {

    public static final Logger LOG = LoggerFactory.getLogger(AnchorIndexingFilter.class);
    private ImmutableConfig conf;
    private boolean deduplicate = false;

    public AnchorIndexingFilter() {
    }

    public AnchorIndexingFilter(ImmutableConfig conf) {
        this.conf = conf;
    }

    public void setup(ImmutableConfig conf) {
        this.conf = conf;

        deduplicate = conf.getBoolean("anchorIndexingFilter.deduplicate", true);
    }

    @Override
    public Params getParams() {
        return Params.of("anchor.indexing.filter.deduplicate", deduplicate);
    }

    /**
     */
    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

    /**
     * The {@link AnchorIndexingFilter} filter object which supports boolean
     * configuration settings for the deduplication of anchors. See
     * {@code anchorIndexingFilter.deduplicate} in pulsar-default.xml.
     *
     * @param doc  The {@link IndexDocument} object
     * @param url  URL to be filtered for anchor text
     * @param page {@link WebPage} object relative to the URL
     * @return filtered IndexDocument
     */
    @Override
    public IndexDocument filter(IndexDocument doc, String url, WebPage page) {
        HashSet<String> set = null;

        for (Entry<CharSequence, CharSequence> e : page.getInlinks().entrySet()) {
            String anchor = e.getValue().toString();

            if (anchor.equals(""))
                continue;

            if (deduplicate) {
                if (set == null)
                    set = new HashSet<>();
                String lcAnchor = anchor.toLowerCase();

                // Check if already processed the current anchor
                if (!set.contains(lcAnchor)) {
                    doc.add("anchor", anchor);

                    // Add to set
                    set.add(lcAnchor);
                }
            } else {
                doc.add("anchor", anchor);
            }
        }

        return doc;
    }
}
