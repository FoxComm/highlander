package utils

import cats.data.Xor
import cats.data.Xor.{left, right}
import monocle.Lens

trait FSM[A, B] { self: B ⇒
  val stateLens: Lens[B, A]
  val fsm: Map[A, Set[A]]

  private def currentState = stateLens.get(this)

  /** Returns the new state, but does not change the model */
  def transitionState(newState: A): Xor[String, A] =
    if (newState == currentState) right(newState)
    else
      fsm.get(currentState) match {
        case Some(states) if states.contains(newState) ⇒ right(newState)
        case _ ⇒ left(s"could not transition from '${currentState}' to '${newState}'")
      }

  /** Returns a Right of a copy of the model in the correct state, or a Left if the state
    * can’t be changed. */
  def transitionTo(newState: A): Xor[String, B] = transitionState(newState).map { newState ⇒
    stateLens.set(newState)(this)
  }
}

