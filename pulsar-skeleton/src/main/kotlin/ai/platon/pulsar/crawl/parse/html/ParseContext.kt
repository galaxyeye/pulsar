package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import org.w3c.dom.DocumentFragment

/**
 * Created by vincent on 17-7-28.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class ParseContext constructor(
        val page: WebPage,
        val parseResult: ParseResult = ParseResult(),
        var document: FeaturedDocument? = null
)
