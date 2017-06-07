package core.db

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.implicits._
import core.failures.{Failure, Failures, GeneralFailure}
import core.utils.Strings._
import core.utils.{friendlyClassName, Validation}

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

  def updateTo(newModel: M): Either[Failures, M] = Either.right(newModel)

  def primarySearchKey: String = id.toString

  def mustBeCreated: Either[Failures, M] =
    if (id == 0)
      Either.left(GeneralFailure(s"Refusing to update unsaved ${friendlyClassName(this)} model").single)
    else Either.right(this)
}
