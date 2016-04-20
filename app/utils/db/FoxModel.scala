package utils.db

import cats.data.Validated.Valid
import cats.data.{ValidatedNel, Xor}
import failures.{Failure, Failures, GeneralFailure}
import monocle.Lens
import utils.Strings._
import utils.Validation

trait FoxModel[M <: FoxModel[M]] extends Validation[M] { self: M ⇒

  type Id = Int

  def id: Id

  def isNew: Boolean = id == 0

  def searchKey(): Option[String] = None

  def modelName: String = getClass.getCanonicalName.lowerCaseFirstLetter

  def validate: ValidatedNel[Failure, M] = Valid(this)

  def sanitize: M = this

  def updateTo(newModel: M): Failures Xor M = Xor.right(newModel)

  // Read-only lens that returns String representation of primary search key value
  def primarySearchKeyLens: Lens[M, String] = Lens[M, String](_.id.toString)(_ ⇒ _ ⇒ this)

  def mustBeCreated: Failures Xor M =
    if (id == 0) Xor.Left(GeneralFailure("Refusing to update unsaved model").single) else Xor.right(this)
}
