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
            //POST v1/taxonomy/{contextName}
            (post & pathEnd & entity(as[CreateTaxonomyPayload])) { payload ⇒
              mutateOrFailures(TaxonomyManager.createTaxonomy(payload))
            } ~
            // v1/taxonomy/{contextName}/{taxonomyFormId}
            pathPrefix(IntNumber) { taxonomyFormId ⇒
              //GET v1/taxonomy/{contextName}/{taxonomyFormId}
              (get & pathEnd) {
                getOrFailures {
                  TaxonomyManager.getTaxonomy(taxonomyFormId)
                }
              } ~
              //PATCH v1/taxonomy/{contextName}/{taxonomyFormId}
              (patch & pathEnd & entity(as[UpdateTaxonomyPayload])) { (payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.updateTaxonomy(taxonomyFormId, payload)
                }
              } ~
              //DELETE v1/taxonomy/{contextName}/{taxonomyFormId}
              (delete & pathEnd) {
                deleteOrFailures {
                  TaxonomyManager.archiveByContextAndId(taxonomyFormId)
                }
              }
            } ~
            //POST v1/taxonomy/{contextName}/{taxonomyFormId}
            (post & pathPrefix(IntNumber) & pathEnd & entity(as[CreateTaxonPayload])) {
              (taxonFormId, payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.createTaxon(taxonFormId, payload)
                }
            } ~
            pathPrefix("taxon") {
              //GET v1/taxonomy/{contextName}/term/{termFormId}
              pathPrefix(IntNumber) { taxonFormId ⇒
                (get & pathEnd) {
                  getOrFailures {
                    TaxonomyManager.getTaxon(taxonFormId)
                  }
                } ~
                //PATCH v1/taxonomy/{contextName}/term/{termFormId}
                (patch & pathEnd & entity(as[UpdateTaxonPayload])) { payload ⇒
                  mutateOrFailures {
                    TaxonomyManager.updateTaxon(taxonFormId, payload)
                  }
                } ~
                //DELETE v1/taxonomy/{contextName}/term/{termFormId}
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
