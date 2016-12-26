package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.TaxonomyPayloads._
import services.Authenticator.AuthData
import services.taxonomy.TaxonomyManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object TaxonomyRoutes {

  def routes(implicit ec: EC,
             db: DB,
             auth: AuthData[User],
             tr: TracingRequest,
             trace: TracingExtensionImpl) = {

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
            }
          }
        }
      } ~
      pathPrefix("taxon") {

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
