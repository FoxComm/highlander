package utils

import cats.data.Xor
import Xor.{left, right}

trait FSM[A] {
  val state: A

  val fsm: Map[A, Set[A]]

  def transition(newState: A): Xor[String, A] = fsm.get(state) match {
    case Some(states) if states.contains(newState) || state == newState ⇒
      right(newState)
    case None if state == newState ⇒
      right(newState)
    case _ ⇒
      left(s"could not transition from '${state}' to '${newState}'")
  }
}

