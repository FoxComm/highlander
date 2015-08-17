package utils

import cats.data.Xor
import Xor.{left, right}

trait FSM[A] {
  def state: A

  def fsm: Map[A, Set[A]]

  def transition(newState: A): Xor[String, A] = fsm.get(state) match {
    case Some(states) if states.contains(newState) ⇒
      right(newState)
    case None if newState == state ⇒
      right(newState)
    case _ ⇒
      left(s"could not transition from '${state}' to '${newState}'")
  }
}

