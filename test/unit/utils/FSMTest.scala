package utils

import cats.data.Xor
import monocle.Lens
import monocle.macros.GenLens
import util.TestBase
import org.scalatest.prop.TableDrivenPropertyChecks._

class FSMTest extends TestBase {

  sealed trait Operation
  case object Pop extends Operation
  case object Lock extends Operation
  case object PopAndLock extends Operation
  case object LockAndPop extends Operation
  case object BreakItDown extends Operation

  case class Robot(state: Operation) extends FSM[Operation, Robot] {
    val stateLens = GenLens[Robot](_.state)

    val fsm: Map[Operation, Set[Operation]] = Map(
      Pop → Set(Lock, LockAndPop),
      Lock → Set(Pop, PopAndLock),
      PopAndLock → Set(Pop, BreakItDown),
      LockAndPop → Set(Lock, BreakItDown)
    )
  }


  "FSM" - {
    "can transition successfully" in {
      Robot(state = Pop).transitionState(Lock) mustBe 'right
    }

    "transitions the model" in {
      val fineRobot = Robot(Pop)

      rightValue(fineRobot.transitionTo(Lock)) { newRobot ⇒
        newRobot.state must === (Lock)
        newRobot       must === (fineRobot.copy(state = Lock))
      }
    }

    "can always transition to identity state" in {
      val states = Table(
        "state",
        Pop,
        Lock,
        PopAndLock,
        LockAndPop,
        BreakItDown
      )

      forAll(states) { case (state) ⇒
        rightValue(Robot(state = state).transitionState(state))(_ must === (state))
      }
    }

    "cannot transition to an invalid state" in {
      Robot(state = Pop).transitionState(PopAndLock) mustBe 'left
    }

    "cannot transition from a state which has no mapping step and is not identity" in {
      Robot(state = BreakItDown).transitionState(Pop) mustBe 'left
    }

    "returns new state upon valid transition" in {
      Robot(state = Pop).transitionState(Lock).fold(identity, _.toString) must ===("Lock")
    }
  }
}
