package utils.apis

import utils.ElasticsearchApi

case class Apis(stripe: FoxStripeApi,
                amazon: AmazonApi,
                middlewarehouse: MiddlewarehouseApi,
                elasticSearch: ElasticsearchApi)
