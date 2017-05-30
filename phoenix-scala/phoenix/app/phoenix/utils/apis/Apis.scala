package phoenix.utils.apis

case class Apis(stripe: FoxStripeApi,
                amazon: AmazonApi,
                middlewarehouse: MiddlewarehouseApi,
                elasticSearch: ElasticsearchApi)
