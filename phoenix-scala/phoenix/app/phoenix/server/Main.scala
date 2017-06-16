package phoenix.server

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.stripe.Stripe
import com.typesafe.scalalogging.LazyLogging
import core.db._
import java.util.Properties
import java.util.concurrent.atomic.AtomicReference

import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.json4s._
import org.json4s.jackson._
import phoenix.models.account.{AccountAccessMethod, Scope, Scopes}
import phoenix.services.Authenticator
import phoenix.services.Authenticator.{requireAdminAuth, UserAuthenticator}
import phoenix.services.account.AccountCreateContext
import phoenix.services.actors._
import phoenix.utils.FoxConfig.config
import phoenix.utils.apis._
import phoenix.utils.http.CustomHandlers
import phoenix.utils.http.HttpLogger.logFailedRequests
import phoenix.utils.{ElasticsearchApi, Environment, FoxConfig}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import slick.jdbc.PostgresProfile.api._

object Main extends App with LazyLogging {
  logger.info("Starting phoenix server")

  try {
    FoxStripe.ping()

    val service = new Service()
    service.performSelfCheck()
    service.bind()
    service.setupRemorseTimers()

    logger.info("Startup process complete")
  } catch {
    case e: Throwable ⇒
      val cause = Option(e.getCause).fold("") { c ⇒
        s"\nCaused by $c"
      }
      logger.error(s"$e$cause\nExiting now!")
      Thread.sleep(1000)
      System.exit(1)
  }
}

object Setup extends LazyLogging {

  implicit val executionContext: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(java.util.concurrent.Executors.newCachedThreadPool())

  lazy val defaultApis: Apis =
    Apis(setupStripe(), new AmazonS3, setupMiddlewarehouse(), setupElasticSearch(), setupKafka())

  def setupStripe(): FoxStripe = {
    logger.info("Loading Stripe API key")
    Stripe.apiKey = config.apis.stripe.key
    logger.info("Successfully set Stripe key")
    new FoxStripe(new StripeWrapper())
  }

  def setupMiddlewarehouse(): Middlewarehouse = {
    logger.info("Setting up MWH...")
    new Middlewarehouse(config.apis.middlewarehouse.url)
  }

  def setupElasticSearch(): ElasticsearchApi = {
    logger.info("Setting up Elastic Search")
    ElasticsearchApi.fromConfig(config)
  }

  def setupKafka(): KafkaProducer[GenericData.Record, GenericData.Record] = {
    logger.info("Setting up Kafka producer for activities")

    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.apis.kafka.bootStrapServersConfig)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.apis.kafka.keySerializer)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.apis.kafka.valueSerializer)
    props.put("schema.registry.url", config.apis.kafka.schemaRegistryURL)
    props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, config.apis.kafka.producerTimeout)

    new KafkaProducer[GenericData.Record, GenericData.Record](props)
  }
}

class Service(systemOverride: Option[ActorSystem] = None,
              dbOverride: Option[Database] = None,
              apisOverride: Option[Apis] = None,
              esOverride: Option[ElasticsearchApi] = None,
              addRoutes: immutable.Seq[Route] = immutable.Seq.empty)(implicit val env: Environment) {

  import FoxConfig.config
  import phoenix.utils.JsonFormatters._

  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: Formats                  = phoenixFormats

  implicit val system: ActorSystem = systemOverride.getOrElse {
    ActorSystem.create("Orders", FoxConfig.unsafe)
  }

  implicit val executionContext: ExecutionContextExecutor = Setup.executionContext

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val logger: LoggingAdapter = Logging(system, getClass)

  implicit val db: Database = dbOverride.getOrElse(Database.forConfig("db", FoxConfig.unsafe))
  implicit val apis: Apis   = apisOverride.getOrElse(Setup.defaultApis)

  private val roleName: String = config.users.customer.role
  private val orgName: String  = config.users.customer.org
  private val scopeId: Int     = config.users.customer.scopeId

  private val scope: Scope = Await
    .result(Scopes.findOneById(scopeId).run(), Duration.Inf)
    .getOrElse(throw new RuntimeException(s"Unable to find a scope with id $scopeId"))

  private val customerCreateContext        = AccountCreateContext(List(roleName), orgName, scopeId)
  implicit val userAuth: UserAuthenticator = Authenticator.forUser(customerCreateContext)

  val defaultRoutes: Route = {
    pathPrefix("v1") {
      phoenix.routes.AuthRoutes.routes(scope.ltree) ~
      phoenix.routes.Public.routes(customerCreateContext, scope.ltree) ~
      phoenix.routes.Customer.routes ~
      requireAdminAuth(userAuth) { implicit auth ⇒
        phoenix.routes.admin.AdminRoutes.routes ~
        phoenix.routes.admin.NotificationRoutes.routes ~
        phoenix.routes.admin.AssignmentsRoutes.routes ~
        phoenix.routes.admin.OrderRoutes.routes ~
        phoenix.routes.admin.CartRoutes.routes ~
        phoenix.routes.admin.CustomerRoutes.routes ~
        phoenix.routes.admin.CustomerGroupsRoutes.routes ~
        phoenix.routes.admin.GiftCardRoutes.routes ~
        phoenix.routes.admin.ReturnRoutes.routes ~
        phoenix.routes.admin.ProductRoutes.routes ~
        phoenix.routes.admin.SkuRoutes.routes ~
        phoenix.routes.admin.VariantRoutes.routes ~
        phoenix.routes.admin.DiscountRoutes.routes ~
        phoenix.routes.admin.PromotionRoutes.routes ~
        phoenix.routes.admin.ImageRoutes.routes ~
        phoenix.routes.admin.CouponRoutes.routes ~
        phoenix.routes.admin.CategoryRoutes.routes ~
        phoenix.routes.admin.GenericTreeRoutes.routes ~
        phoenix.routes.admin.StoreAdminRoutes.routes ~
        phoenix.routes.admin.ObjectRoutes.routes ~
        phoenix.routes.admin.PluginRoutes.routes ~
        phoenix.routes.admin.TaxonomyRoutes.routes ~
        phoenix.routes.admin.CatalogRoutes.routes ~
        phoenix.routes.admin.ProductReviewRoutes.routes ~
        phoenix.routes.admin.ShippingMethodRoutes.routes ~
        phoenix.routes.service.MigrationRoutes.routes(customerCreateContext, scope.ltree) ~
        pathPrefix("service") {
          phoenix.routes.service.PaymentRoutes.routes ~ //Migrate this to auth with service tokens once we have them
          phoenix.routes.service.CustomerGroupRoutes.routes
        }
      }
    }
  }

  lazy val devRoutes: Route = {
    pathPrefix("v1") {
      requireAdminAuth(userAuth) { implicit auth ⇒
        phoenix.routes.admin.DevRoutes.routes
      }
    }
  }

  val allRoutes: Route = {
    val routes = if (!env.isProd) {
      logger.info("Activating dev routes")
      addRoutes.foldLeft(defaultRoutes ~ devRoutes)(_ ~ _)
    } else
      addRoutes.foldLeft(defaultRoutes)(_ ~ _)
    logFailedRequests(routes, logger)
  }

  implicit def rejectionHandler: RejectionHandler = CustomHandlers.jsonRejectionHandler

  implicit def exceptionHandler: ExceptionHandler = CustomHandlers.jsonExceptionHandler

  private final val serverBinding =
    new AtomicReference[Option[ServerBinding]](Option.empty[ServerBinding])

  def bind(config: FoxConfig = config): Future[ServerBinding] = {
    val host = config.http.interface
    val port = config.http.port
    val bind = Http().bindAndHandle(allRoutes, host, port).map { binding ⇒
      serverBinding.set(Some(binding))
      binding
    }
    logger.info(s"Bound to $host:$port")
    bind
  }

  def close(): Future[Unit] =
    serverBinding.getAndSet(Option.empty[ServerBinding]).fold(Future.successful({}))(_.unbind())

  def setupRemorseTimers(): Unit = {
    logger.info("Scheduling remorse timer")
    val remorseTimer      = system.actorOf(Props(new RemorseTimer()), "remorse-timer")
    val remorseTimerBuddy = system.actorOf(Props(new RemorseTimerMate()), "remorse-timer-mate")
    system.scheduler
      .schedule(Duration.Zero, 1.minute, remorseTimer, Tick)(executionContext, remorseTimerBuddy)
  }

  def performSelfCheck(): Unit = {
    logger.info("Performing self check")
    if (config.auth.method == FoxConfig.AuthMethod.Jwt) {
      import phoenix.models.auth.Keys
      assert(Keys.loadPrivateKey.isSuccess, "Can't load private key")
      assert(Keys.loadPublicKey.isSuccess, "Can't load public key")
    }
    logger.info(s"Using password hash algorithm: ${AccountAccessMethod.passwordsHashAlgorithm}")
    logger.info("Self check complete")
  }
}
