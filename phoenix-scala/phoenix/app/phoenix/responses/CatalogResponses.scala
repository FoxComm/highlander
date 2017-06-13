package phoenix.responses

import java.time.Instant

import phoenix.models.catalog.Catalog
import phoenix.models.location.Country

object CatalogResponse {
  case class Root(id: Int,
                  name: String,
                  site: Option[String],
                  countryId: Int,
                  countryName: String,
                  defaultLanguage: String,
                  createdAt: Instant,
                  updatedAt: Instant)

  def build(catalog: Catalog, country: Country): Root =
    Root(
      id = catalog.id,
      name = catalog.name,
      site = catalog.site,
      countryId = country.id,
      countryName = country.name,
      defaultLanguage = catalog.defaultLanguage,
      createdAt = catalog.createdAt,
      updatedAt = catalog.updatedAt
    )
}
