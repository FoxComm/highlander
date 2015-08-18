package utils

import cats.data.Xor
import cats.data.Xor.{left, right}
import monocle.Lens

trait FSM[A, B] { self: B ⇒
  val state: A
  val stateLens: Lens[B, A]

  val fsm: Map[A, Set[A]]

  def transition(newState: A): Xor[String, A] = fsm.get(state) match {
    case Some(states) if states.contains(newState) || state == newState ⇒
      right(newState)
    case None if state == newState ⇒
      right(newState)
    case _ ⇒
      left(s"could not transition from '${state}' to '${newState}'")
  }

  def transitionTo(newState: A): Xor[String, B] = transition(newState).map { newState ⇒
    stateLens.set(newState)(this)
  }
}

