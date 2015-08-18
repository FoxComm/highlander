package utils

import util.TestBase
import org.scalatest.prop.TableDrivenPropertyChecks._

class FSMTest extends TestBase {

  sealed trait Operation
  case object Pop extends Operation
  case object Lock extends Operation
  case object PopAndLock extends Operation
  case object LockAndPop extends Operation
  case object BreakItDown extends Operation

  case class Robot(state: Operation) extends FSM[Operation] {
    val fsm: Map[Operation, Set[Operation]] = Map(
      Pop → Set(Lock, LockAndPop),
      Lock → Set(Pop, PopAndLock),
      PopAndLock → Set(Pop, BreakItDown),
      LockAndPop → Set(Lock, BreakItDown)
    )
  }

  "FSM" - {
    "can transition successfully" in {
      Robot(state = Pop).transition(Lock) mustBe 'right
    }

    "can always transition to identity state" in {
      val states = Table(
        ("state"),
        (Pop),
        (Lock),
        (PopAndLock),
        (LockAndPop),
        (BreakItDown)
      )

      forAll(states) { case (state) ⇒
        Robot(state = state).transition(state) mustBe 'right
      }
    }

    "cannot transition to an invalid state" in {
      Robot(state = Pop).transition(PopAndLock) mustBe 'left
    }

    "cannot transition from a state which has no mapping step and is not identity" in {
      Robot(state = BreakItDown).transition(Pop) mustBe 'left
    }

    "returns new state upon valid transition" in {
      Robot(state = Pop).transition(Lock).fold(identity, _.toString) must ===("Lock")
    }
  }
}
