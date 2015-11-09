package utils

import cats.data.Xor
import monocle.Lens
import services.{Failures, StatusTransitionNotAllowed}

trait FSM[S, M <: FSM[S, M]] { self: M ⇒
  /* this is a def because a val confuses jackson somehow for json rendering
      see: https://github.com/FoxComm/phoenix-scala/pull/276/files#r37361391
   */
  def stateLens: Lens[M, S]
  def primarySearchKeyLens: Lens[M, String]

  val fsm: Map[S, Set[S]]

  private def currentState = stateLens.get(this)

  def transitionState(newState: S): Failures Xor M =
    if (newState == currentState) Xor.right(this)
    else fsm.get(currentState) match {
      case Some(states) if states.contains(newState) ⇒
        Xor.right(stateLens.set(newState)(this))
      case _ ⇒
        val searchKey = primarySearchKeyLens.get(this)
        Xor.left(StatusTransitionNotAllowed(self, currentState.toString, newState.toString, searchKey).single)
      }

  def transitionAllowed(newState: S): Boolean = transitionState(newState).isRight

  def transitionModel(newModel: M): Failures Xor M =
    transitionState(newModel.stateLens.get(newModel)).map(_ ⇒ newModel)
}
