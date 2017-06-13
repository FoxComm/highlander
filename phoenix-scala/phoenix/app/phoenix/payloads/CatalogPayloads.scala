package phoenix.payloads

object CatalogPayloads {
  case class CreateCatalogPayload(scope: Option[String] = None,
                                  name: String,
                                  site: Option[String],
                                  countryId: Int,
                                  defaultLanguage: String)

  case class UpdateCatalogPayload(name: Option[String] = None,
                                  site: Option[String] = None,
                                  countryId: Option[Int] = None,
                                  defaultLanguage: Option[String] = None)

  case class AddProductsPayload(productIds: Seq[Int])
}
