package models.payment

import cats.data.Xor
import com.pellucid.sealerate
import failures.Failures
import shapeless.lens
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.db.FoxModel
import utils.{ADT, FSM}

trait ExternalCharge[M <: FoxModel[M] with FSM[ExternalCharge.State, M]]
    extends FoxModel[M]
    with FSM[ExternalCharge.State, M] {
  self: M ⇒
  import ExternalCharge._
  import shapeless._

  val chargeId: String
  val state: State

  override def updateTo(newModel: M): Failures Xor M =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
      Cart →
        Set(Auth),
      Auth →
        Set(FullCapture, FailedCapture, CanceledAuth, ExpiredAuth),
      ExpiredAuth →
        Set(Auth)
  )
}

object ExternalCharge {

  sealed trait State
  case object Cart          extends State
  case object Auth          extends State
  case object FailedAuth    extends State
  case object ExpiredAuth   extends State
  case object CanceledAuth  extends State
  case object FailedCapture extends State
  case object FullCapture   extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

}
