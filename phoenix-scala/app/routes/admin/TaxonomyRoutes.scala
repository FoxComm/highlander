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
          adminObjectContext(contextName)(db, ec) { implicit context ⇒
            //POST v1/taxonomy/{contextName}
            (post & pathEnd & entity(as[CreateTaxonPayload])) { payload ⇒
              mutateOrFailures(TaxonomyManager.createTaxon(payload))
            } ~
            // v1/taxonomy/{contextName}/{taxonomyFormId}
            pathPrefix(IntNumber) { taxonFormId ⇒
              //GET v1/taxonomy/{contextName}/{taxonomyFormId}
              (get & pathEnd) {
                getOrFailures {
                  TaxonomyManager.getTaxon(taxonFormId)
                }
              } ~
              //PATCH v1/taxonomy/{contextName}/{taxonomyFormId}
              (patch & pathEnd & entity(as[UpdateTaxonPayload])) { (payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.updateTaxon(taxonFormId, payload)
                }
              } ~
              //DELETE v1/taxonomy/{contextName}/{taxonomyFormId}
              (delete & pathEnd) {
                deleteOrFailures {
                  TaxonomyManager.archiveByContextAndId(taxonFormId)
                }
              }
            } ~
            //POST v1/taxonomy/{contextName}/{taxonomyFormId}
            (post & pathPrefix(IntNumber) & pathEnd & entity(as[CreateTermPayload])) {
              (taxonFormId, payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.createTerm(taxonFormId, payload)
                }
            } ~
            pathPrefix("term") {
              //GET v1/taxonomy/{contextName}/term/{termFormId}
              pathPrefix(IntNumber) { termFormId ⇒
                (get & pathEnd) {
                  getOrFailures {
                    TaxonomyManager.getTerm(termFormId)
                  }
                }
              } ~
              //PATCH v1/taxonomy/{contextName}/term/{termFormId}
              (patch & pathPrefix(IntNumber) & pathEnd & entity(as[UpdateTermPayload])) {
                (termId, payload) ⇒
                  mutateOrFailures {
                    TaxonomyManager.updateTerm(termId, payload)
                  }
              } ~
              //DELETE v1/taxonomy/{contextName}/term/{termFormId}
              (delete & pathPrefix(IntNumber) & pathEnd) { termId ⇒
                deleteOrFailures {
                  TaxonomyManager.archiveTermByContextAndId(termId)
                }
              }
            }
          }
        }
      }
    }
  }
}
