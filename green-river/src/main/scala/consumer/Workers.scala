package consumer

import scala.concurrent.Future
import consumer.activity._
import consumer.aliases._
import consumer.elastic.ElasticSearchProcessor
import consumer.elastic.ScopeProcessor
import consumer.elastic.ScopedIndexer
import consumer.elastic.mappings._
import consumer.elastic.mappings.admin._
import consumer.utils.PhoenixConnectionInfo

object Workers {
  def activityWorker(conf: MainConfig, connectionInfo: PhoenixConnectionInfo)(
      implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC): Future[Unit] = Future {
    // Init
    val activityConnectors = Seq(AccountConnector,
                                 OrderConnector,
                                 GiftCardConnector,
                                 SharedSearchConnector,
                                 StoreCreditConnector,
                                 ProductConnector,
                                 SkuConnector,
                                 PromotionConnector,
                                 CouponConnector)

    val activityProcessor = new ActivityProcessor(connectionInfo, activityConnectors)

    val avroProcessor = new AvroProcessor(
        schemaRegistryUrl = conf.avroSchemaRegistryUrl, processor = activityProcessor)

    val consumer = new MultiTopicConsumer(topics = Seq(conf.activityTopic),
                                          broker = conf.kafkaBroker,
                                          groupId = s"${conf.kafkaGroupId}_activity",
                                          processor = avroProcessor,
                                          startFromBeginning = conf.startFromBeginning)

    // Start consuming & processing
    Console.out.println(s"Reading activities from broker ${conf.kafkaBroker}")
    consumer.readForever()
  }

  def searchViewWorkers(conf: MainConfig, connectionInfo: PhoenixConnectionInfo)(
      implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC): Future[Unit] = Future {

    val transformers = topicTransformers(connectionInfo)
    val indexTopics  = conf.indexTopics

    val futures = indexTopics.flatMap {
      case (index, topics) ⇒ {
          topics.map { topic ⇒
            Future {

              val maybeTransformer = transformers.get(topic)

              maybeTransformer match {
                case Some(transformer) ⇒
                  // Init
                  val esProcessor = new ElasticSearchProcessor(uri = conf.elasticSearchUrl,
                                                               cluster = conf.elasticSearchCluster,
                                                               indexName = index,
                                                               topics = Seq(topic),
                                                               jsonTransformers =
                                                                 Map(topic → transformer))

                  val avroProcessor =
                    new AvroProcessor(schemaRegistryUrl = conf.avroSchemaRegistryUrl,
                                      processor = esProcessor)

                  val consumer = new MultiTopicConsumer(topics = Seq(topic),
                                                        broker = conf.kafkaBroker,
                                                        groupId = s"${conf.kafkaGroupId}_$topic",
                                                        processor = avroProcessor,
                                                        startFromBeginning =
                                                          conf.startFromBeginning)

                  // Start consuming & processing
                  Console.out.println(s"Reading from broker ${conf.kafkaBroker}")
                  consumer.readForever()
                case None ⇒
                  throw new IllegalArgumentException(
                      s"The Topic '$topic' does not have a json transformer")
              }
            }
          }
        }
    }

    Future.sequence(futures)
  }

  def scopedSearchViewWorkers(conf: MainConfig, connectionInfo: PhoenixConnectionInfo)(
      implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC): Future[Unit] = Future {

    val transformers = topicTransformers(connectionInfo)
    val indexTopics  = conf.scopedIndexTopics
    val ADMIN_INDEX  = "admin"
    val SCOPES_TABLE = "scopes"

    val scopeProcessorFuture = Future {

      val scopedProcessor = new ScopeProcessor(uri = conf.elasticSearchUrl,
                                               cluster = conf.elasticSearchCluster,
                                               indexTopics = indexTopics,
                                               jsonTransformers = transformers)

      val avroProcessor = new AvroProcessor(schemaRegistryUrl = conf.avroSchemaRegistryUrl,
                                            processor = scopedProcessor)

      val consumer = new MultiTopicConsumer(topics = Seq(SCOPES_TABLE),
                                            broker = conf.kafkaBroker,
                                            groupId = s"${conf.kafkaGroupId}_scopes",
                                            processor = avroProcessor,
                                            startFromBeginning = conf.startFromBeginning)

      // Start consuming & processing
      Console.out.println(s"Scope Processor reading from broker ${conf.kafkaBroker}")
      consumer.readForever()
    }

    val indexers = indexTopics.flatMap {
      case (index, topics) ⇒ {
          topics.map { topic ⇒
            Future {

              val maybeTransformer = transformers.get(topic)

              maybeTransformer match {
                case Some(transformer) ⇒
                  // Init
                  val esProcessor = new ScopedIndexer(uri = conf.elasticSearchUrl,
                                                      cluster = conf.elasticSearchCluster,
                                                      indexName = index,
                                                      topics = Seq(topic),
                                                      jsonTransformers = Map(topic → transformer))

                  val avroProcessor =
                    new AvroProcessor(schemaRegistryUrl = conf.avroSchemaRegistryUrl,
                                      processor = esProcessor)

                  val consumer =
                    new MultiTopicConsumer(topics = Seq(topic),
                                           broker = conf.kafkaBroker,
                                           groupId = s"scoped_${conf.kafkaGroupId}_$topic",
                                           processor = avroProcessor,
                                           startFromBeginning = conf.startFromBeginning)

                  // Start consuming & processing
                  Console.out.println(
                      s"Scoped $topic Processor Reading from broker ${conf.kafkaBroker}")
                  consumer.readForever()
                case None ⇒
                  throw new IllegalArgumentException(
                      s"The Topic '$topic' does not have a json transformer")
              }
            }
          }
        }
    }

    Future.sequence(Seq(scopeProcessorFuture) ++ indexers)
  }

  def topicTransformers(connectionInfo: PhoenixConnectionInfo)(
      implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC) = Map(
      "carts_search_view"                     → CartsSearchView(),
      "countries"                             → CountriesSearchView(),
      "customer_items_view"                   → CustomerItemsView(),
      "customers_search_view"                 → CustomersSearchView(),
      "customer_groups_search_view"           → CustomerGroupsSearchView(),
      "failed_authorizations_search_view"     → FailedAuthorizationsSearchView(),
      "inventory_search_view"                 → InventorySearchView(),
      "inventory_transactions_search_view"    → InventoryTransactionSearchView(),
      "notes_search_view"                     → NotesSearchView(),
      "orders_search_view"                    → OrdersSearchView(),
      "products_catalog_view"                 → ProductsCatalogView(),
      "products_search_view"                  → ProductsSearchView(),
      "promotions_search_view"                → PromotionsSearchView(),
      "coupons_search_view"                   → CouponsSearchView(),
      "coupon_codes_search_view"              → CouponCodesSearchView(),
      "regions"                               → RegionsSearchView(),
      "sku_search_view"                       → SkuSearchView(),
      "gift_card_transactions_view"           → GiftCardTransactionsSearchView(),
      "gift_cards_search_view"                → GiftCardsSearchView(),
      "store_admins_search_view"              → StoreAdminsSearchView(),
      "store_credit_transactions_search_view" → StoreCreditTransactionsSearchView(),
      "store_credits_search_view"             → StoreCreditsSearchView(),
      "activity_connections_view"             → ActivityConnectionTransformer(connectionInfo),
      "taxonomies_search_view"                → TaxonomiesSearchView(),
      "taxons_search_view"                    → TaxonsSearchView()
  )
}
