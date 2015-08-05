package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import akka.http.scaladsl.model.StatusCodes._
import org.scalactic._
import payloads._
import responses.FullOrder.Response
import responses.{AllOrders, AllOrdersWithFailures, AdminNotes, FullOrder}
import services._
import slick.driver.PostgresDriver.api._
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
        (get & pathEnd) {
          complete {
            models.GiftCards.sortBy(_.id.desc).result.run().map(render(_))
          }
        } ~
        (get & path(IntNumber) & pathEnd) { giftCardId ⇒
          complete {
            renderOrNotFound(GiftCards.findById(giftCardId).run())
          }
        }
      } ~
      pathPrefix("customers") {
        (get & pathEnd) {
          complete {
            models.Customers.sortBy(_.firstName.desc).result.run().map(render(_))
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          complete {
            renderOrNotFound(models.Customers.findById(customerId))
          }
        } ~
        (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          complete {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin).map(renderGoodOrFailures)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd) {
            complete {
              Addresses._findAllByCustomerIdWithStates(customerId).result.run().map { records ⇒
                render(responses.Addresses.build(records))
              }
            }
          } ~
          (post & entity(as[CreateAddressPayload]) & pathEnd) { payload =>
            complete {
              AddressManager.create(payload, customerId).map(renderGoodOrFailures)
            }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultShippingAddress]) & pathEnd) {
            (id, payload) ⇒
              complete {
                AddressManager.setDefaultShippingAddress(customerId, id).map { optFailure ⇒
                  optFailure.fold(HttpResponse(OK)) { f ⇒ renderFailure(Seq(f)) }
                }
              }
          } ~
          (delete & path("default")  & pathEnd) {
            complete {
              AddressManager.removeDefaultShippingAddress(customerId).map { _ ⇒ noContentResponse }
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
                result.map(renderGoodOrFailures)
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
                whenFoundDispatchToService(StoreCredits.findById(storeCreditId).run()) { sc ⇒
                  CustomerCreditConverter.toGiftCard(sc, customerId)
                }
              }
            }
          }
        }
      } ~
      pathPrefix("orders") {
        (get & pathEnd) {
          complete {
            AllOrders.findAll
          }
        } ~
        (patch & entity(as[BulkUpdateOrdersPayload]) & pathEnd) { payload ⇒
          complete {
            for {
              failures ← OrderUpdater.updateMultipleOrders(payload)
              orders ← AllOrders.findAll
            } yield AllOrdersWithFailures(orders, failures)
          }
        }
      } ~
      pathPrefix("orders" / """([a-zA-Z0-9-_]*)""".r) { refNum ⇒
        (get & pathEnd) {
          complete {
            whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
              FullOrder.fromOrder(order).map(Good(_))
            }
          }
        } ~
        (patch & entity(as[UpdateOrderPayload])) { payload ⇒
          complete {
            def finder = Orders.findByRefNum(refNum)
            whenFound(finder.result.headOption.run()) { order ⇒
              OrderUpdater.updateSingleOrder(order, finder, payload)
            }
          }
        } ~
        (post & path("checkout")) {
          complete {
            whenFoundDispatchToService(Orders.findByRefNum(refNum).result.headOption.run()) { order => new Checkout(order).checkout }
          }
        } ~
        (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
          complete {
            whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order =>
              LineItemUpdater.updateQuantities(order, reqItems).flatMap {
                case Good(_) ⇒ FullOrder.fromOrder(order).map(Good(_))
                case Bad(e) ⇒ Future.successful(Bad(e))
              }
            }
          }
        } ~
        pathPrefix("payment-methods") {
          (get & pathEnd) {
            complete {
              renderOrNotFound(Orders.findByRefNum(refNum).result.headOption.run())
            }
          } ~
          (post & path("credit-card") & entity(as[CreateCreditCard])) { reqPayment =>
            complete {
              Orders.findByRefNum(refNum).result.headOption.run().flatMap {
                case None => Future.successful(notFoundResponse)
                case Some(order) =>
                  findCustomer(order.customerId).flatMap {
                    case None =>
                      Future.successful(render("Guest checkout!!"))

                    case Some(customer) =>
                      CreditCardPaymentCreator.run(order, customer, reqPayment).map(renderGoodOrFailures)
                  }
              }
            }
          }
        } ~
        pathPrefix("notes") {
          (get & pathEnd) {
            complete {
              whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒ AdminNotes.forOrder(order) }
            }
          } ~
          (post & entity(as[payloads.CreateNote])) { payload ⇒
            complete {
              whenFoundDispatchToService(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
                services.NoteManager.createOrderNote(order, admin, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
            complete {
              whenFoundDispatchToService(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
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
        } ~
        pathPrefix("shipping-address") {
          (post & entity(as[payloads.CreateShippingAddress]) & pathEnd) { payload ⇒
            complete {
              whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
                services.OrderUpdater.createShippingAddress(order, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            complete {
              Orders.findByRefNum(refNum).result.headOption.run().flatMap {
                case Some(order) ⇒
                  services.OrderUpdater.removeShippingAddress(order.id).map { _ ⇒ noContentResponse }
                case None ⇒
                  Future.successful(notFoundResponse)
              }
            }
          }
        }
      } ~
      pathPrefix("states") {
        (get & pathEnd) {
          complete {
            models.States.sortBy(_.name.asc).result.run().map(render(_))
          }
        }
      }
    }
  }
}

