package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import models.objects.{ProductTaxonLinks, SkuTaxonLinks}
import payloads.TaxonomyPayloads._
import services.Authenticator.AuthData
import services.taxonomy.TaxonomyManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object TaxonomyRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("taxonomy") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (post & pathEnd & entity(as[CreateTaxonomyPayload])) { payload ⇒
              mutateOrFailures(TaxonomyManager.createTaxonomy(payload))
            } ~
            pathPrefix(IntNumber) { taxonomyFormId ⇒
              (get & pathEnd) {
                getOrFailures {
                  TaxonomyManager.getTaxonomy(taxonomyFormId)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateTaxonomyPayload])) { (payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.updateTaxonomy(taxonomyFormId, payload)
                }
              } ~
              (delete & pathEnd) {
                deleteOrFailures {
                  TaxonomyManager.archiveByContextAndId(taxonomyFormId)
                }
              }
            } ~
            (post & pathPrefix(IntNumber) & pathEnd & entity(as[CreateTaxonPayload])) {
              (taxonFormId, payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.createTaxon(taxonFormId, payload)
                }
            } ~
            pathPrefix("taxon") {

              pathPrefix(IntNumber) { taxonFormId ⇒
                (get & pathEnd) {
                  getOrFailures {
                    TaxonomyManager.getTaxon(taxonFormId)
                  }
                } ~
                (patch & pathEnd & entity(as[UpdateTaxonPayload])) { payload ⇒
                  mutateOrFailures {
                    TaxonomyManager.updateTaxon(taxonFormId, payload)
                  }
                } ~
                (delete & pathEnd) {
                  deleteOrFailures {
                    TaxonomyManager.archiveTaxonByContextAndId(taxonFormId)
                  }
                } ~
                pathPrefix("product" / IntNumber) { productFormId ⇒
                  (patch & pathEnd) {
                    mutateOrFailures(TaxonomyManager.assignTaxonTo(productFormId,
                                                                   taxonFormId,
                                                                   ProductTaxonLinks))
                  } ~
                  (delete & pathEnd) {
                    mutateOrFailures(TaxonomyManager.unassignTaxonFrom(productFormId,
                                                                       taxonFormId,
                                                                       ProductTaxonLinks))
                  }
                } ~
                pathPrefix("sku" / IntNumber) { skuFormId ⇒
                  (patch & pathEnd) {
                    mutateOrFailures(
                        TaxonomyManager.assignTaxonTo(skuFormId, taxonFormId, SkuTaxonLinks))
                  } ~
                  (delete & pathEnd) {
                    mutateOrFailures(
                        TaxonomyManager.unassignTaxonFrom(skuFormId, taxonFormId, SkuTaxonLinks)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
