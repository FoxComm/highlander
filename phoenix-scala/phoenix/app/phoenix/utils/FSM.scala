package phoenix.utils

import cats.implicits._
import core.failures.Failures
import phoenix.failures.StateTransitionNotAllowed
import shapeless._

trait FSM[S, M <: FSM[S, M]] { self: M ⇒
  /* this is a def because a val confuses jackson somehow for json rendering
      see: https://github.com/FoxComm/phoenix-scala/pull/276/files#r37361391
   */
  def stateLens: Lens[M, S]
  def primarySearchKey: String

  val fsm: Map[S, Set[S]]

  private def currentState = stateLens.get(this)

  def transitionState(newState: S): Either[Failures, M] =
    if (newState == currentState) Either.right(this)
    else
      fsm.get(currentState) match {
        case Some(states) if states.contains(newState) ⇒
          Either.right(stateLens.set(this)(newState))
        case _ ⇒
          Either.left(StateTransitionNotAllowed(self,
                                                currentState.toString,
                                                newState.toString,
                                                primarySearchKey).single)
      }

  def transitionAllowed(newState: S): Boolean = transitionState(newState).isRight

  def transitionModel(newModel: M): Either[Failures, M] =
    transitionState(newModel.stateLens.get(newModel)).map(_ ⇒ newModel)
}
