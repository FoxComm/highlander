package phoenix.utils.apis

import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.Producer
import phoenix.utils.ElasticsearchApi

case class Apis(stripe: FoxStripeApi,
                amazon: AmazonApi,
                middlewarehouse: MiddlewarehouseApi,
                elasticSearch: ElasticsearchApi,
                kafka: Producer[GenericData.Record, GenericData.Record])
