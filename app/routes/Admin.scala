package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.scalactic._
import payloads._
import responses.{AdminNotes, FullOrder}
import services._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.RunOnDbIO

object Admin {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin]) = {
    import Json4sSupport._
    import utils.Http._

    def findCustomer(id: Int): Future[Option[models.Customer]] = {
      Future.successful(Some(models.Customer(id = id, email = "donkey@donkey.com", password = "donkeyPass",
        firstName = "Mister", lastName = "Donkey")))
    }

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin =>
      pathPrefix("gift-cards") {
        (get & path(IntNumber) & pathEnd) { giftCardId ⇒
          complete {
            renderOrNotFound(GiftCards.findById(giftCardId).run())
          }
        }
      } ~
      pathPrefix("users" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          complete {
            renderOrNotFound(models.Customers.findById(customerId))
          }
        } ~
        (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          complete {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin).map(renderGoodOrBad)
          }
        } ~
        pathPrefix("addresses") {
          get {
            complete {
              Addresses.findAllByCustomerId(customerId).map(render(_))
            }
          } ~
          (post & entity(as[Seq[CreateAddressPayload]])) { payload =>
            complete {
              whenFound(findCustomer(customerId)) { customer =>
                Addresses.createFromPayload(customer, payload)
              }
            }
          }
        } ~
        pathPrefix("payment-methods") {
          pathPrefix("credit-cards") {
            (get & pathEnd) {
              complete {
                render(CreditCards.findAllByCustomerId(customerId))
              }
            } ~
            (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultCreditCard])) { (cardId, payload) ⇒
              complete {
                val result = CustomerManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
                result.map(renderGoodOrBad)
              }
            }
          } ~
          pathPrefix("store-credits") {
            (get & pathEnd) {
              complete {
                renderOrNotFound(StoreCredits.findAllByCustomerId(customerId).map(Some(_)))
              }
            } ~
            (get & path(IntNumber)) { storeCreditId ⇒
              complete {
                renderOrNotFound(StoreCredits.findById(storeCreditId).run())
              }
            } ~
              //              (post & entity(as[CreateStoreCredit])) { payload ⇒
              //                complete {
              //                  Future.successful(HttpResponse(OK))
              //                }
              //              } ~
            (post & path(IntNumber / "convert")) { storeCreditId ⇒
              complete {
                whenFound(StoreCredits.findById(storeCreditId).run()) { sc ⇒
                  CustomerCreditConverter.toGiftCard(sc, customerId)
                }
              }
            }
          }
        }
      } ~
      pathPrefix("orders" / IntNumber) { orderId ⇒
        (get & pathEnd) {
          complete {
            renderOrNotFound(FullOrder.findById(orderId))
          }
        } ~
        (patch & entity(as[UpdateOrderPayload])) { payload =>
          complete {
            whenFound(Orders.findById(orderId).run()) { order =>
              OrderUpdater.updateStatus(order, payload).flatMap {
                case Good(o) ⇒
                  FullOrder.fromOrder(o).map {
                    case Some(r) ⇒ Good(r)
                    case None ⇒ Bad(List("order not found"))
                  }

                case Bad(e) ⇒
                  Future.successful(Bad(e))
              }
            }
          }
        } ~
        (post & path("checkout")) {
          complete {
            whenFound(Orders.findById(orderId).run()) { order => new Checkout(order).checkout }
          }
        } ~
        (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
          complete {
            whenFound(Orders.findById(orderId).run()) { order =>
              LineItemUpdater.updateQuantities(order, reqItems).flatMap {
                case Good(_) ⇒
                  FullOrder.fromOrder(order).map {
                    case Some(r) ⇒ Good(r)
                    case None ⇒ Bad(List("order not found"))
                  }

                case Bad(e) ⇒
                  Future.successful(Bad(e))
              }
            }
          }
        } ~
        pathPrefix("payment-methods") {
          (get & pathEnd) {
            complete {
              renderOrNotFound(Orders.findById(orderId).run())
            }
          } ~
          (post & path("credit-card") & entity(as[CreditCardPayload])) { reqPayment =>
            complete {
              Orders.findById(orderId).run().flatMap {
                case None => Future.successful(notFoundResponse)
                case Some(order) =>
                  findCustomer(order.customerId).flatMap {
                    case None =>
                      Future.successful(render("Guest checkout!!"))

                    case Some(customer) =>
                      CreditCardPaymentCreator.run(order, customer, reqPayment).map(renderGoodOrBad)
                  }
              }
            }
          }
        } ~
        pathPrefix("notes") {
          (get & pathEnd) {
            complete {
              whenFound(Orders.findById(orderId).run()) { order ⇒ AdminNotes.forOrder(order) }
            }
          } ~
          (post & entity(as[payloads.CreateNote])) { payload ⇒
            complete {
              whenFound(Orders.findById(orderId).run()) { order ⇒
                services.NoteManager.createOrderNote(order, admin, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
            complete {
              whenFound(Orders.findById(orderId).run()) { order ⇒
                services.NoteManager.updateNote(noteId, admin, payload)
              }
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            complete {
              notFoundResponse
            }
          }
          //            (patch & entity(as[payloads.UpdateNote])) { payload ⇒
          //              complete {
          //                whenFound(Orders.findById(orderId).run()) { order ⇒
          //                  NoteCreator.createOrderNote(order, admin, payload)
          //                }
          //              }
          //            }
        }
      }
    }
  }
}

