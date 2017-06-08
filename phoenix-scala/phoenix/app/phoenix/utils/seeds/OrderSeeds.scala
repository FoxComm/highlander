package phoenix.utils.seeds

import com.github.tminglei.slickpg.LTree
import phoenix.models.Note
import phoenix.models.account.Scope
import phoenix.models.cord.Order._
import phoenix.models.cord._
import phoenix.utils.aliases._

trait OrderSeeds {

  def order(scope: LTree): Order =
    Order(accountId = 1, referenceNumber = "ABCD1234-11", scope = scope, state = ManualHold, contextId = 1)

  def cart(scope: LTree): Cart =
    Cart(accountId = 1, referenceNumber = "ABCD1234-11", scope = scope)

  def orderNotes(implicit au: AU): Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body, scope = Scope.current)
    Seq(
      newNote("This customer is a donkey."),
      newNote("No, seriously."),
      newNote("Like, an actual donkey."),
      newNote("How did a donkey even place an order on our website?")
    )
  }
}
