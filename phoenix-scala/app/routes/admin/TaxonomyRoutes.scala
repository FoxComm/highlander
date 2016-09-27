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
            //GET /v1/taxonomy/{contextName}/{taxonomyFormId}
            pathPrefix(IntNumber) { taxonFormId ⇒
              (get & pathEnd) {
                getOrFailures {
                  TaxonomyManager.getTaxon(taxonFormId)
                }
              } ~
              //POST /v1/taxonomy/{contextName}
              (post & pathEnd & entity(as[CreateTaxonPayload])) { payload ⇒
                mutateOrFailures {
                  TaxonomyManager.createTaxon(payload)
                }
              } ~
              //PATCH /v1/taxonomy/{contextName}/{taxonomyFormId}
              (patch & pathEnd & pathPrefix(IntNumber) & entity(as[UpdateTaxonPayload])) {
                (taxonomyId, payload) ⇒
                  mutateOrFailures {
                    TaxonomyManager.updateTaxon(taxonomyId, payload)
                  }
              } ~
              //DELETE /v1/taxonomy/{contextName}/{taxonomyFormId}
              (delete & pathPrefix(IntNumber) & pathEnd) { taxonomyId ⇒
                deleteOrFailures {
                  TaxonomyManager.archiveByContextAndId(taxonomyId)
                }
              }
            } ~
            pathPrefix("term") {
              //GET /v1/taxonomy/{contextName}/term/{termFormId}
              pathPrefix(IntNumber) { termFormId ⇒
                (get & pathEnd) {
                  getOrFailures {
                    TaxonomyManager.getTerm(termFormId)
                  }
                } ~
                //POST /v1/taxonomy/{contextName}/{taxonomyFormId}
                (post & pathEnd & entity(as[CreateTermPayload])) { payload ⇒
                  mutateOrFailures {
                    TaxonomyManager.createTerm(payload)
                  }
                } ~
                //PATCH /v1/taxonomy/{contextName}/term/{termFormId}
                (patch & pathEnd & pathPrefix(IntNumber) & entity(as[UpdateTermPayload])) {
                  (termId, payload) ⇒
                    mutateOrFailures {
                      TaxonomyManager.updateTerm(termId, payload)
                    }
                } ~
                //DELETE /v1/taxonomy/{contextName}/term/{termFormId}
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
}
