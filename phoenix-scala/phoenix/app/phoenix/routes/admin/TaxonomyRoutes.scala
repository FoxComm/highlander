package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.TaxonPayloads._
import phoenix.payloads.TaxonomyPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.taxonomy.TaxonomyManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object TaxonomyRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
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
              } ~
              pathPrefix("taxons") {
                (post & pathEnd & entity(as[CreateTaxonPayload])) { payload ⇒
                  mutateOrFailures {
                    TaxonomyManager.createTaxon(taxonomyFormId, payload)
                  }
                }
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
