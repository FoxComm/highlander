package phoenix.utils.apis

import phoenix.utils.ElasticsearchApi

case class Apis(stripe: FoxStripeApi,
                amazon: AmazonApi,
                middlewarehouse: MiddlewarehouseApi,
                elasticSearch: ElasticsearchApi)
