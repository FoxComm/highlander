package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.TaxonomyPayloads._
import services.taxonomy.TaxonomyManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object TaxonomyRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
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
                }
              }
            }
          }
        }
      }
    }
  }
}
