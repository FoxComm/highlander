package utils.db

import cats.data.Validated.Valid
import cats.data.{ValidatedNel, Xor}
import failures.{Failure, Failures, GeneralFailure}
import utils.Strings._
import utils.{Validation, friendlyClassName}

trait Identity[A] { self: A ⇒
  type Id = Int
  def id: Id
}

trait FoxModel[M <: FoxModel[M]] extends Validation[M] with Identity[M] { self: M ⇒

  def isNew: Boolean = id == 0

  def searchKey(): Option[String] = None

  def modelName: String = getClass.getCanonicalName.lowerCaseFirstLetter

  def validate: ValidatedNel[Failure, M] = Valid(this)

  def sanitize: M = this

  def updateTo(newModel: M): Failures Xor M = Xor.right(newModel)

  def primarySearchKey: String = id.toString

  def mustBeCreated: Failures Xor M =
    if (id == 0)
      Xor.Left(
        GeneralFailure(s"Refusing to update unsaved ${friendlyClassName(this)} model").single)
    else Xor.right(this)
}
