package phoenix.services.catalog

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import core.db._
import phoenix.failures.CatalogFailures._
import phoenix.models.account._
import phoenix.models.catalog._
import phoenix.models.location._
import phoenix.payloads.CatalogPayloads._
import phoenix.responses.CatalogResponse
import phoenix.services.LogActivity
import phoenix.utils.aliases._

object CatalogManager extends LazyLogging {

  def getCatalog(id: Int)(implicit ec: EC): DbResultT[CatalogResponse] =
    for {
      catalogWithCountry ← * <~ Catalogs.filterWithCountry(id).mustFindOneOr(CatalogNotFound(id))
    } yield (CatalogResponse.build _).tupled(catalogWithCountry)

  def createCatalog(
      payload: CreateCatalogPayload)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[CatalogResponse] =
    for {
      scope   ← * <~ Scope.resolveOverride(payload.scope)
      catalog ← * <~ Catalogs.create(Catalog.build(payload, scope))
      country ← * <~ Countries.mustFindById400(catalog.countryId)
      response = CatalogResponse.build(catalog, country)
      _ ← * <~ LogActivity().withScope(scope).catalogCreated(au.model, response)
    } yield response

  def updateCatalog(
      catalogId: Int,
      payload: UpdateCatalogPayload)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[CatalogResponse] =
    for {
      existing ← * <~ Catalogs.mustFindById404(catalogId)
      catalog  ← * <~ Catalogs.update(existing, Catalog.build(existing, payload))
      country  ← * <~ Countries.mustFindById400(catalog.countryId)
      response = CatalogResponse.build(catalog, country)
      _ ← * <~ LogActivity().withScope(ac.ctx.scope).catalogUpdated(au.model, response)
    } yield response

  def addProductsToCatalog(
      catalogId: Int,
      payload: AddProductsPayload)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[CatalogResponse] =
    for {
      catalog ← * <~ getCatalog(catalogId)
      _       ← * <~ CatalogProducts.createAll(CatalogProduct.buildSeq(catalog.id, payload.productIds))
      _ ← * <~ LogActivity()
           .withScope(ac.ctx.scope)
           .productsAddedToCatalog(au.model, catalog, payload.productIds)
    } yield catalog

  def removeProductFromCatalog(catalogId: Int,
                               productId: Int)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[Unit] =
    for {
      catalogProduct ← * <~ CatalogProducts
                        .filterProduct(catalogId, productId)
                        .mustFindOneOr(ProductNotFoundInCatalog(catalogId, productId))
      _ ← * <~ CatalogProducts.update(catalogProduct, catalogProduct.copy(archivedAt = Some(Instant.now)))
      _ ← * <~ LogActivity().withScope(ac.ctx.scope).productRemovedFromCatalog(au.model, catalogId, productId)
    } yield ()
}
