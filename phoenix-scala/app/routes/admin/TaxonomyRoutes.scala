package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.TaxonomyPayloads._
import services.Authenticator.AuthData
import services.taxonomy.TaxonomyManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object TaxonomyRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("taxonomies") {
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
              (taxonomyFormId, payload) ⇒
                mutateOrFailures {
                  TaxonomyManager.createTaxon(taxonomyFormId, payload)
                }
            }
          }
        }
      } ~
      pathPrefix("taxons") {

        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
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
                  mutateOrFailures(TaxonomyManager.assignProduct(taxonFormId, productFormId))
                } ~
                (delete & pathEnd) {
                  mutateOrFailures(TaxonomyManager.unassignProduct(taxonFormId, productFormId))
                }
              }
            }
          }
        }
      }
    }
  }
}
