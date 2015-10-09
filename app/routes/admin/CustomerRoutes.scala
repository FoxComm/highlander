package routes.admin

import akka.http.scaladsl.model.{HttpResponse, ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.stage.{Context, PushPullStage}
import akka.util.ByteString
import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._
import org.json4s.{Formats, jackson}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.Slick.implicits._
import utils.CustomDirectives._

import scala.concurrent.{ExecutionContext, Future}

object CustomerRoutes {

  import utils.JsonFormatters._

  implicit lazy val serialization: Serialization.type = jackson.Serialization
  implicit lazy val formats:       Formats = phoenixFormats

  class ToJsonArray[T <: AnyRef]
    extends PushPullStage[T, ByteString] {
    private var first = true

    override def onPush(elem: T, ctx: Context[ByteString]) = {
      val leading = if (first) { first = false; "[" } else ","
      ctx.push(ByteString(leading + json(elem) + "\n"))
    }

    override def onPull(ctx: Context[ByteString]) =
      if (ctx.isFinishing) {
        if (first) ctx.pushAndFinish(ByteString("[]"))
        else ctx.pushAndFinish(ByteString("]"))
      } else ctx.pull()

    override def onUpstreamFinish(ctx: Context[ByteString]) =
      ctx.absorbTermination()
  }

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("customers") {
        (get & pathEnd) {
          goodOrFailures {
            CustomerManager.findAll
          }
        }
      } ~
      // TODO ************* the following is just a POC and introduction to a discussion *************
      pathPrefix("customersPaged") {
        (get & pathEnd & sortAndPage) { (sort, page) ⇒
          good {
            val query = models.Customers
            val sortedQuery = sort match {
              case Some(s) ⇒
                query.sortBy { table ⇒
                  // TODO: of course here should be a column name validation and a proper column type selection
                  if(s.asc) table.column[String](s.sortColumn).asc else table.column[String](s.sortColumn).desc
                }
              case None    ⇒ query
            }
            val pagedQuery = page match {
              case Some(p) ⇒
                sortedQuery.drop(p.pageSize * (p.pageNo - 1)).take(p.pageSize)
              case None    ⇒ sortedQuery
            }
            pagedQuery.result.run() // TODO probably we will have a root JSON object with the paging metadata
          }
        }
      } ~
      pathPrefix("customersReactive") {
        (get & pathEnd & sortAndStart) { (sort, start) ⇒

          val query = models.Customers
          val sortedQuery = sort match {
            case Some(s) ⇒
              query.sortBy { table ⇒
                // TODO: of course here should be a column name validation and a proper column type selection
                if(s.asc) table.column[String](s.sortColumn).asc else table.column[String](s.sortColumn).desc
              }
            case None    ⇒ query
          }
          val queryWithStartElement = start match {
            case Some(s) ⇒
              sortedQuery.drop(s.startFrom - 1)
            case None    ⇒ sortedQuery
          }
          val stream = Source(implicitly[Database].stream(queryWithStartElement.result))
            .transform(() => new ToJsonArray)
          // TODO probably we will have a root JSON object with the paging metadata
          complete(HttpResponse(entity = HttpEntity.Chunked.fromData(ContentTypes.`application/json`, stream)))
        }
      } ~
        // TODO *************************** end of the POC ***************************
      pathPrefix("customers" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          goodOrFailures {
            CustomerManager.getById(customerId)
          }
        } ~
        (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          goodOrFailures {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd) {
            good {
              Addresses._findAllByCustomerIdWithRegions(customerId).result.run().map { records ⇒
                responses.Addresses.build(records)
              }
            }
          } ~
          (post & entity(as[CreateAddressPayload]) & pathEnd) { payload ⇒
            goodOrFailures {
              AddressManager.create(payload, customerId)
            }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultShippingAddress]) & pathEnd) {
            (id, payload) ⇒
              nothingOrFailures {
                AddressManager.setDefaultShippingAddress(customerId, id)
              }
          } ~
          (delete & path("default") & pathEnd) {
            nothingOrFailures {
              AddressManager.removeDefaultShippingAddress(customerId)
            }
          } ~
          (patch & path(IntNumber) & entity(as[CreateAddressPayload]) & pathEnd) { (addressId, payload) ⇒
            goodOrFailures {
              AddressManager.edit(addressId, customerId, payload)
            }
          } ~
          (get & path("display") & pathEnd) {
            complete {
              Customers._findById(customerId).result.headOption.run().flatMap {
                case None           ⇒ Future.successful(notFoundResponse)
                case Some(customer) ⇒ AddressManager.getDisplayAddress(customer).map(renderOrNotFound(_))
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          (get & pathEnd) {
            good { CreditCardManager.creditCardsInWalletFor(customerId) }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultCreditCard]) & pathEnd) {
            (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
              }
          } ~
          (post & entity(as[payloads.CreateCreditCard]) & pathEnd) { payload ⇒
            complete {
              whenFound(Customers.findById(customerId)) { customer ⇒
                CreditCardManager.createCardThroughGateway(customer, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.EditCreditCard]) & pathEnd) { (cardId, payload) ⇒
            nothingOrFailures {
              CreditCardManager.editCreditCard(customerId, cardId, payload)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            nothingOrFailures {
              CreditCardManager.deleteCreditCard(customerId = customerId, id = cardId)
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (get & pathEnd) {
            complete {
              whenFound(Customers.findById(customerId)) { customer ⇒
                StoreCredits.findAllByCustomerId(customer.id).map(Xor.right)
              }
            }
          } ~
          (post & entity(as[payloads.CreateManualStoreCredit])) { payload ⇒
            goodOrFailures {
              StoreCreditService.createManual(admin, customerId, payload)
            }
          } ~
          (post & path(IntNumber / "convert")) { storeCreditId ⇒
            complete {
              whenFoundDispatchToService(StoreCredits.findById(storeCreditId).run()) { sc ⇒
                CustomerCreditConverter.toGiftCard(sc, customerId)
              }
            }
          }
        }
      }
    }
  }
}
