package ai.platon.pulsar.ql

import ai.platon.pulsar.BasicPulsarSession
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.context.support.BasicPulsarContext
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.persist.WebDb
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.context.support.StaticApplicationContext

open class BasicSQLContext(
    applicationContext: AbstractApplicationContext
) : AbstractSQLContext(applicationContext) {

    private val log = LoggerFactory.getLogger(BasicSQLContext::class.java)

    override fun createSession(sessionDelegate: SessionDelegate): AbstractSQLSession {
        val session = sqlSessions.computeIfAbsent(sessionDelegate) {
            BasicSQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
        }
        log.info("Session is created | #{}/{}", session.id, id)
        return session as BasicSQLSession
    }

    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}

class StaticSQLContext(
    override val applicationContext: StaticApplicationContext = StaticApplicationContext()
) : BasicSQLContext(applicationContext) {
    /**
     * The unmodified config
     * */
    override val unmodifiedConfig = getBeanOrNull() ?: ImmutableConfig()
    /**
     * Url normalizers
     * */
    override val urlNormalizers = getBeanOrNull() ?: UrlNormalizers(unmodifiedConfig)
    /**
     * The web db
     * */
    override val webDb = getBeanOrNull() ?: WebDb(unmodifiedConfig)
    /**
     * The global cache
     * */
    override val globalCache = getBeanOrNull() ?: GlobalCache(unmodifiedConfig)
    /**
     * The inject component
     * */
    override val injectComponent = getBeanOrNull() ?: InjectComponent(webDb, unmodifiedConfig)
    /**
     * The fetch component
     * */
    override val fetchComponent = getBeanOrNull() ?: BatchFetchComponent(webDb, unmodifiedConfig)
    /**
     * The update component
     * */
    override val updateComponent = getBeanOrNull() ?: UpdateComponent(webDb, unmodifiedConfig)
    /**
     * The load component
     * */
    override val loadComponent = getBeanOrNull() ?: LoadComponent(webDb, globalCache, fetchComponent, updateComponent, unmodifiedConfig)

    init {
        applicationContext.refresh()
    }
}

open class ClassPathXmlSQLContext(configLocation: String) :
    AbstractSQLContext(ClassPathXmlApplicationContext(configLocation)) {

    private val log = LoggerFactory.getLogger(ClassPathXmlSQLContext::class.java)

    override fun createSession(sessionDelegate: SessionDelegate): AbstractSQLSession {
        val session = sqlSessions.computeIfAbsent(sessionDelegate) {
            BasicSQLSession(this, sessionDelegate, SessionConfig(sessionDelegate, unmodifiedConfig))
        }
        log.info("Session is created | #{}/{}", session.id, id)
        return session as BasicSQLSession
    }

    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}

open class DefaultClassPathXmlSQLContext() : ClassPathXmlSQLContext(
    System.getProperty(
        CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION,
        AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
    )
)

object SQLContexts {
    @Synchronized
    fun activate(): SQLContext = (PulsarContexts.activate() as? SQLContext)
        ?: activate(DefaultClassPathXmlSQLContext())

    @Synchronized
    fun activate(context: AbstractSQLContext): SQLContext = context.also { PulsarContexts.activate(it) }

    @Synchronized
    fun activate(context: ApplicationContext): SQLContext =
        activate(BasicSQLContext(context as AbstractApplicationContext))

    @Synchronized
    fun activate(contextLocation: String): SQLContext = activate(ClassPathXmlSQLContext(contextLocation))

    @Synchronized
    fun shutdown() = PulsarContexts.shutdown()
}

fun withSQLContext(block: (context: SQLContext) -> Unit) {
    SQLContexts.activate(DefaultClassPathXmlSQLContext()).use {
        block(it)
    }
}

fun withSQLContext(contextLocation: String, block: (context: SQLContext) -> Unit) {
    SQLContexts.activate(ClassPathXmlSQLContext(contextLocation)).use {
        block(it)
    }
}

fun withSQLContext(applicationContext: ApplicationContext, block: (context: SQLContext) -> Unit) {
    SQLContexts.activate(applicationContext).use {
        block(it)
    }
}
