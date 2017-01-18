package utils

import cats.data.Xor
import failures.Failures
import org.scalatest.prop.TableDrivenPropertyChecks._
import shapeless._
import testutils.TestBase
import utils.db._

class FSMTest extends TestBase {

  sealed trait Operation
  case object Pop         extends Operation
  case object Lock        extends Operation
  case object PopAndLock  extends Operation
  case object LockAndPop  extends Operation
  case object BreakItDown extends Operation

  case class Robot(id: Int = 0, state: Operation)
      extends FoxModel[Robot]
      with FSM[Operation, Robot] {
    def stateLens = lens[Robot].state
    override def updateTo(newModel: Robot): Failures Xor Robot =
      super.transitionModel(newModel)

    val fsm: Map[Operation, Set[Operation]] = Map(
      Pop        → Set(Lock, LockAndPop),
      Lock       → Set(Pop, PopAndLock),
      PopAndLock → Set(Pop, BreakItDown),
      LockAndPop → Set(Lock, BreakItDown)
    )
  }

  "FSM" - {
    "can transition successfully" in {
      Robot(state = Pop).transitionState(Lock) mustBe 'right
    }

    "transitions the model" in {
      val fineRobot = Robot(state = Pop)
      val newRobot  = rightValue(fineRobot.transitionState(Lock))

      newRobot.state must === (Lock)
      newRobot must === (fineRobot.copy(state = Lock))
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

      forAll(states) {
        case (state) ⇒
          val robot = Robot(state = state)
          rightValue(robot.transitionState(state)) must === (robot)
      }
    }

    "cannot transition to an invalid state" in {
      Robot(state = Pop).transitionState(PopAndLock) mustBe 'left
    }

    "cannot transition from a state which has no mapping step and is not identity" in {
      Robot(state = BreakItDown).transitionState(Pop) mustBe 'left
    }

    "decides if it can transition to another model" in {
      Robot(state = Pop).updateTo(Robot(state = Lock)) mustBe 'right
      Robot(state = Pop).updateTo(Robot(state = PopAndLock)) mustBe 'left
    }
  }
}
