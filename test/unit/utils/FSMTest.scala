package utils

import models.{CreditCardCharge, GiftCard, Order}
import util.TestBase

class FSMTest extends TestBase {

  sealed trait Operation
  case object Pop extends Operation
  case object Lock extends Operation
  case object PopAndLock extends Operation
  case object LockAndPop extends Operation
  case object BreakItDown extends Operation

  case class Robot(state: Operation) extends FSM[Operation] {
    def fsm: Map[Operation, Set[Operation]] = Map(
      Pop → Set(Lock, LockAndPop),
      Lock → Set(Pop, PopAndLock),
      PopAndLock → Set(Pop, BreakItDown),
      LockAndPop → Set(Lock, BreakItDown),
      BreakItDown → Set(Pop, Lock, PopAndLock, LockAndPop)
    )
  }

  "FSM" - {
    "can transition" in {
      Robot(state = Pop).transition(Lock) mustBe 'right
      Robot(state = Pop).transition(BreakItDown) mustBe 'left
    }
  }
}
