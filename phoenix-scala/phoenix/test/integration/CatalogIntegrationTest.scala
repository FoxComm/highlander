import core.db._
import phoenix.models.catalog._
import phoenix.payloads.CatalogPayloads._
import phoenix.responses.CatalogResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._

class CatalogIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestObjectContext
    with ApiFixtures {

  "GET /v1/catalogs/:id" - {
    "succeeeds" in new Catalog_ApiFixture {
      val response = catalogsApi(catalog.id).get().as[CatalogResponse.Root]
      response.name must === ("default")
      response.countryName must === ("United States")
    }
  }

  "POST /v1/catalogs" - {
    "succeeds with simple payload" in new Catalog_ApiFixture {
      val payload = CreateCatalogPayload(name = "Japan",
                                         site = Some("stage.foxcommerce.jp"),
                                         countryId = 115,
                                         defaultLanguage = "jp")

      val response = catalogsApi.create(payload).as[CatalogResponse.Root]
      response.name must === ("Japan")
      response.countryName must === ("Japan")
    }

    "succeeds even with a duplicate name" in new Catalog_ApiFixture {
      val payload =
        CreateCatalogPayload(name = "default", site = None, countryId = 234, defaultLanguage = "en")

      val response = catalogsApi.create(payload).as[CatalogResponse.Root]
      response.name must === ("default")
      response.countryName must === ("United States")
    }

    "fails with an invalid country" in new Catalog_ApiFixture {
      val payload =
        CreateCatalogPayload(name = "will fail", site = None, countryId = 10001, defaultLanguage = "en")

      // TODO: Jeff - Add validation for the error message. Right now, it's a terribly ugly database error.
      catalogsApi.create(payload).mustHaveStatus(400)
    }
  }

  "PATCH /v1/catalogs/:id" - {
    "succeeds in changing the name" in new Catalog_ApiFixture {
      val payload  = UpdateCatalogPayload(name = Some("revised"))
      val response = catalogsApi(catalog.id).update(payload).as[CatalogResponse.Root]

      response.name must === ("revised")
      response.site must === (catalog.site)
      response.defaultLanguage === (catalog.defaultLanguage)
    }

    "succeeds in changing the site" in new Catalog_ApiFixture {
      val payload  = UpdateCatalogPayload(site = Some("tumi.foxcommerce.com"))
      val response = catalogsApi(catalog.id).update(payload).as[CatalogResponse.Root]
      response.site must === (Some("tumi.foxcommerce.com"))
    }

    "succeeds in deleting the site" in new Catalog_ApiFixture {
      val payload  = UpdateCatalogPayload(site = Some(""))
      val response = catalogsApi(catalog.id).update(payload).as[CatalogResponse.Root]
      response.site must === (None)
    }
  }

  "POST /v1/catalogs/:id/products" - {
    "succeeds in adding the product to the catalog" in new Catalog_ApiFixture {
      val product = ProductSku_ApiFixture().product
      val payload = AddProductsPayload(productIds = Seq(product.id))
      catalogsApi(catalog.id).addProducts(payload).mustBeOk

      val catalogProduct = CatalogProducts.filterProduct(catalog.id, product.id).gimme.head
      catalogProduct.archivedAt must === (None)
    }

    "fails with invalid catalog" in new Catalog_ApiFixture {
      val product = ProductSku_ApiFixture().product
      val payload = AddProductsPayload(productIds = Seq(product.id))
      catalogsApi(100).addProducts(payload).mustHaveStatus(404)
    }

    "fails with invalid product" in new Catalog_ApiFixture {
      val payload = AddProductsPayload(productIds = Seq(100))
      catalogsApi(catalog.id).addProducts(payload).mustHaveStatus(400)
    }
  }

  "DELETE /v1/catalogs/:id/products" - {
    "succeeds in removing a product to the catalog" in new Catalog_ApiFixture {
      val product = ProductSku_ApiFixture().product
      val payload = AddProductsPayload(productIds = Seq(product.id))
      catalogsApi(catalog.id).addProducts(payload).mustBeOk
      catalogsApi(catalog.id).deleteProduct(product.id)

      val catalogProduct = CatalogProducts.filterProduct(catalog.id, product.id).gimme.head
      catalogProduct.archivedAt must !==(None)
    }

    "fails to remove a product that was not in a catalog" in new Catalog_ApiFixture {
      val product = ProductSku_ApiFixture().product
      catalogsApi(catalog.id).deleteProduct(product.id).mustHaveStatus(404)
    }

    "fails with invalid catalog" in new Catalog_ApiFixture {
      val product = ProductSku_ApiFixture().product
      catalogsApi(100).deleteProduct(product.id).mustHaveStatus(404)
    }

    "fails with invalid product" in new Catalog_ApiFixture {
      val product = ProductSku_ApiFixture().product
      catalogsApi(100).deleteProduct(product.id).mustHaveStatus(404)
    }

  }

}
