package utils.db

import cats.data.Validated.Valid
import cats.data.{ValidatedNel, Xor}
import failures.{Failure, Failures, GeneralFailure}
import shapeless._
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

  def primarySearchKey: String = id.toString

  def mustBeCreated: Failures Xor M =
    if (id == 0) Xor.Left(GeneralFailure("Refusing to update unsaved model").single)
    else Xor.right(this)
}
