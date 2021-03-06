package ai.platon.pulsar.parse.html.filters

import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.deprecated.EntityOptions
import ai.platon.pulsar.crawl.parse.AbstractParseFilter
import ai.platon.pulsar.crawl.parse.FilterResult
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.html.JsoupExtractor
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.DomStatistics
import org.slf4j.LoggerFactory

/**
 * Created by vincent on 16-9-14.
 *
 * Parse Web page using Jsoup if and only if WebPage.query is specified
 *
 * Selector filter, Css selector, XPath selector and Scent selectors are supported
 */
@Deprecated("use x-sql instead")
class PathExtractor(
        val conf: ImmutableConfig
) : AbstractParseFilter() {

    companion object {
        enum class Counter { jsoupFailure, noEntity, brokenEntity, brokenSubEntity }
        init { AppMetrics.reg.register(Counter::class.java) }
    }

    private var log = LoggerFactory.getLogger(PathExtractor::class.java)

    private val enumCounters = AppMetrics.reg.enumCounterRegistry

    /**
     * Extract all fields in the page
     */
    override fun doFilter(parseContext: ParseContext): FilterResult {
        val page = parseContext.page
        val extractor = JsoupExtractor(page, conf)

        val document = parseContext.document?: extractor.parse()
        parseContext.document = document

        val query = page.query?: page.args
        val options = EntityOptions.parse(query)
        if (!options.hasRules()) {
            return FilterResult.success(ParseStatus.SUCCESS_EXT)
        }

        val fieldCollections = extractor.extractAll(options)
        if (fieldCollections.isEmpty()) {
            return FilterResult.success()
        }

        // All last extracted fields are cleared, so we just keep the last extracted fields
        // TODO: How to save updated comments?
        // We only save comments extracted from the current page
        // Comments appears in sub pages can not be read in this WebPage, they may be crawled as separated WebPages
        val pageModel = page.pageModel
        var fields = fieldCollections[0]
        val majorGroup = pageModel.emplace(1, 0, "selector", fields.map)
        var loss = fields.loss

        page.pageCounters.set(Self.missingFields, loss)
        enumCounters.inc(Counter.brokenEntity, if (loss > 0) 1 else 0)

        var brokenSubEntity = 0
        for (i in 1 until fieldCollections.size) {
            fields = fieldCollections[i]
            pageModel.emplace(10000 + i, majorGroup.id.toInt(), "selector-sub", fields.map)
            loss = fields.loss
            if (loss > 0) {
                ++brokenSubEntity
            }
        }

        page.pageCounters.set(Self.brokenSubEntity, brokenSubEntity)
        enumCounters.inc(Counter.brokenSubEntity, brokenSubEntity)

        return FilterResult.success()
    }

    private fun collectPageFeatures(page: WebPage, document: FeaturedDocument, parseResult: ParseResult) {
        val url = page.url

        // log.debug("Parsing amazon page | {}", url)
        val stat = DomStatistics()

        document.document.body().forEachElement { e ->
            if (e.isImage) {
                ++stat.img
                if (e.parent().width in 150..350 && e.parent().height in 150..350) {
                    ++stat.mediumImg
                }
            } else if (e.isAnchor) {
                ++stat.anchor
            }

            if (e.isAnchorImage) {
                ++stat.anchorImg
            } else if (e.isImageAnchor) {
                ++stat.imgAnchor
            }

            if (!e.isBlock && e.tagName() !in arrayOf("a", "img")) {
                return@forEachElement
            }
        }
    }
}
