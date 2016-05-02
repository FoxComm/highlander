package consumer

import scala.concurrent.Future

import consumer.activity._
import consumer.aliases._
import consumer.elastic.ElasticSearchProcessor
import consumer.elastic.mappings._
import consumer.utils.PhoenixConnectionInfo

object Workers {
  def activityWorker(conf: MainConfig, connectionInfo: PhoenixConnectionInfo)
    (implicit ec: EC, ac: AS, mat: AM, cp: CP): Future[Unit] = Future {
    // Init
    val activityConnectors = Seq(AdminConnector(), CustomerConnector(), OrderConnector(),
      GiftCardConnector(), SharedSearchConnector(), StoreCreditConnector(), ProductConnector(), SkuConnector())

    val activityProcessor = new ActivityProcessor(connectionInfo, activityConnectors)

    val avroProcessor = new AvroProcessor(schemaRegistryUrl = conf.avroSchemaRegistryUrl, processor = activityProcessor)

    val consumer = new MultiTopicConsumer(topics = Seq(conf.activityTopic), broker = conf.kafkaBroker,
      groupId = s"${conf.kafkaGroupId}_activity", processor = avroProcessor, startFromBeginning = conf.startFromBeginning)

    // Start consuming & processing
    Console.out.println(s"Reading activities from broker ${conf.kafkaBroker}")
    consumer.readForever()
  }

  def searchViewWorkers(conf: MainConfig, connectionInfo: PhoenixConnectionInfo)
    (implicit ec: EC, ac: AS, mat: AM, cp: CP): Future[Unit] = Future {

      val transformers = topicTransformers(conf, connectionInfo)
      val topics = conf.topicsPlusActivity()
      val futures = topics.map { topic ⇒ 
        Future { 

          val maybeTransformer = transformers.get(topic)

          maybeTransformer match { 
            case Some(transformer) ⇒  {
              // Init
              val esProcessor = new ElasticSearchProcessor(uri = conf.elasticSearchUrl, cluster = conf.elasticSearchCluster,
                indexName = conf.elasticSearchIndex, topics = Seq(topic),
                jsonTransformers = Map(topic → transformer))

              val avroProcessor = new AvroProcessor(schemaRegistryUrl = conf.avroSchemaRegistryUrl, processor = esProcessor)

              val consumer = new MultiTopicConsumer(topics = Seq(topic), broker = conf.kafkaBroker,
                groupId = s"${conf.kafkaGroupId}_${topic}", processor = avroProcessor, startFromBeginning = conf.startFromBeginning)

              // Start consuming & processing
              Console.out.println(s"Reading from broker ${conf.kafkaBroker}")
              consumer.readForever()
            }
            case None ⇒ throw new IllegalArgumentException(s"The Topic '$topic' does not have a json transformer")
          }
        }
      }

      Future.sequence(futures)
  }

  def topicTransformers(conf: MainConfig, connectionInfo: PhoenixConnectionInfo)
    (implicit ec: EC, ac: AS, mat: AM, cp: CP) = Map(
    "countries_search_view"             → CountriesSearchView(),
    "customer_items_view"               → CustomerItemsView(),
    "customers_search_view"             → CustomersSearchView(),
    "failed_authorizations_search_view" → FailedAuthorizationsSearchView(),
    "inventory_search_view"             → InventorySearchView(),
    "inventory_transactions_search_view" → InventoryTransactionSearchView(),
    "notes_search_view"                 → NotesSearchView(),
    "orders_search_view"                → OrdersSearchView(),
    "products_catalog_view"             → ProductsCatalogView(),
    "products_search_view"              → ProductsSearchView(),
    "promotions_search_view"            → PromotionsSearchView(),
    "coupons_search_view"               → CouponsSearchView(),
    "coupon_codes_search_view"          → CouponCodesSearchView(),
    "regions_search_view"               → RegionsSearchView(),
    "sku_search_view"                   → SkuSearchView(),
    "gift_card_transactions_view"       → GiftCardTransactionsSearchView(),
    "gift_cards_search_view"            → GiftCardsSearchView(),
    "store_admins_search_view"          → StoreAdminsSearchView(),
    "store_credit_transactions_view"    → StoreCreditTransactionsSearchView(),
    "store_credits_search_view"         → StoreCreditsSearchView(),
    conf.connectionTopic                → ActivityConnectionTransformer(connectionInfo)
  )
}
