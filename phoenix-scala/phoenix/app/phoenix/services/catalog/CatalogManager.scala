package phoenix.services.catalog

import cats.data._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import phoenix.models.account._
import phoenix.models.catalog._
import phoenix.models.location._
import phoenix.payloads.CatalogPayloads._
import phoenix.responses.CatalogResponse._
import phoenix.utils.aliases._
import core.db._
import phoenix.failures.CatalogFailures.CatalogNotFound
import phoenix.services.LogActivity

object CatalogManager extends LazyLogging {

  def getCatalog(id: Int)(implicit ec: EC): DbResultT[Root] =
    for {
      catalogWithCountry ← * <~ Catalogs.filterWithCountry(id).mustFindOneOr(CatalogNotFound(id))
    } yield (build _).tupled(catalogWithCountry)

  def createCatalog(payload: CreateCatalogPayload)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[Root] =
    for {
      scope   ← * <~ Scope.resolveOverride(payload.scope)
      catalog ← * <~ Catalogs.create(Catalog.build(payload, scope))
      country ← * <~ Countries.mustFindById400(catalog.countryId)
      response = build(catalog, country)
      _ ← * <~ LogActivity().withScope(scope).catalogCreated(au.model, response)
    } yield response

  def updateCatalog(catalogId: Int,
                    payload: UpdateCatalogPayload)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[Root] =
    for {
      existing ← * <~ Catalogs.mustFindById404(catalogId)
      catalog  ← * <~ Catalogs.update(existing, Catalog.build(existing, payload))
      country  ← * <~ Countries.mustFindById400(catalog.countryId)
      response = build(catalog, country)
      _ ← * <~ LogActivity().withScope(ac.ctx.scope).catalogUpdated(au.model, response)
    } yield response
}
