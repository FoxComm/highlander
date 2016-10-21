package utils.seeds

import com.github.tminglei.slickpg.LTree
import models.Note
import models.cord.Order._
import models.cord._
import utils.aliases._

import com.github.tminglei.slickpg.LTree

trait OrderSeeds {

  def order(scope: LTree): Order =
    Order(accountId = 1,
          referenceNumber = "ABCD1234-11",
          scope = scope,
          state = ManualHold,
          contextId = 1)

  def cart(scope: LTree): Cart =
    Cart(accountId = 1, referenceNumber = "ABCD1234-11", scope = scope)

  def orderNotes: Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    Seq(
        newNote("This customer is a donkey."),
        newNote("No, seriously."),
        newNote("Like, an actual donkey."),
        newNote("How did a donkey even place an order on our website?")
    )
  }
}
