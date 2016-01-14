import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.activity.ActivityContext
import models.{Notes, _}
import responses.AdminNotes
import services.{NotFoundFailure404, NoteManager}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._
import utils.time._

class OrderNotesIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")

  "POST /v1/notes/order/:refNum" - {
    "can be created by an admin for an order" in new Fixture {
      val response = POST(s"v1/notes/order/${order.referenceNumber}",
        payloads.CreateNote(body = "Hello, FoxCommerce!"))

      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]

      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/notes/order/${order.referenceNumber}", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (List("body must not be empty"))
    }

    "returns a 404 if the order is not found" in new Fixture {
      val response = POST(s"v1/notes/order/ABACADSF113", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.NotFound)
      parseErrors(response) must === (NotFoundFailure404(Order, "ABACADSF113").description)
    }
  }

  "GET /v1/notes/order/:refNum" - {
    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createOrderNote(order.refNum, storeAdmin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/notes/order/${order.referenceNumber}")
      response.status must === (StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]

      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/order/:refNum/:noteId" - {
    "can update the body text" in new Fixture {
      val rootNote = NoteManager.createOrderNote(order.refNum, storeAdmin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.rightVal

      val response = PATCH(s"v1/notes/order/${order.referenceNumber}/${rootNote.id}",
        payloads.UpdateNote(body = "donkey"))
      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("donkey")
    }
  }

  "DELETE /v1/notes/order/:refNum/:noteId" - {
    "can soft delete note" in new Fixture {
      val note = rightValue(NoteManager.createOrderNote(order.refNum, storeAdmin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue)

      val response = DELETE(s"v1/notes/order/${order.referenceNumber}/${note.id}")
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === (1)
      updatedNote.deletedAt.value.isBeforeNow === (true)

      // Deleted note should not be returned
      val allNotesResponse = GET(s"v1/notes/order/${order.referenceNumber}")
      allNotesResponse.status must === (StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = GET(s"v1/notes/order/${order.referenceNumber}/${note.id}")
      getDeletedNoteResponse.status must === (StatusCodes.NotFound)
    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      order      ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, status = Order.Cart))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).runTxn().futureValue.rightVal
  }
}
