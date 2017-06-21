package phoenix.models.payment

import com.pellucid.sealerate
import core.failures.Failures
import phoenix.models.payment.ExternalCharge.State
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import core.db._
import phoenix.utils.{ADT, FSM}

trait ExternalChargeVals {
  val stripeChargeId: String
  val state: State
}

trait ExternalCharge[Model <: FoxModel[Model] with FSM[ExternalCharge.State, Model]]
    extends FoxModel[Model]
    with ExternalChargeVals
    with FSM[ExternalCharge.State, Model] { self: Model ⇒
  import ExternalCharge._

  override def updateTo(newModel: Model): Either[Failures, Model] =
    super.transitionModel(newModel)

  def updateModelState(s: State)(implicit ec: EC): DbResultT[Unit]

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
