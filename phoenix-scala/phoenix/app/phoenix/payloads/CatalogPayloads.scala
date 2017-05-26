package phoenix.payloads

object CatalogPayloads {
  case class CreateCatalogPayload(scope: Option[String] = None,
                                  name: String,
                                  site: Option[String],
                                  countryId: Int,
                                  defaultLanguage: String)

  case class UpdateCatalogPayload(name: Option[String],
                                  site: Option[String],
                                  countryId: Option[Int],
                                  defaultLanguage: Option[String])
}
