package utils

import cats.data.Xor
import cats.data.Xor.{left, right}
import monocle.Lens

trait FSM[A, B] { self: B ⇒
  val stateLens: Lens[B, A]
  val fsm: Map[A, Set[A]]

  private def currentState = stateLens.get(this)

  def transition(newState: A): Xor[String, A] = fsm.get(currentState) match {
    case Some(states) if states.contains(newState) || currentState == newState ⇒
      right(newState)
    case None if currentState == newState ⇒
      right(newState)
    case _ ⇒
      left(s"could not transition from '${currentState}' to '${newState}'")
  }

  def transitionTo(newState: A): Xor[String, B] = transition(newState).map { newState ⇒
    stateLens.set(newState)(this)
  }
}

