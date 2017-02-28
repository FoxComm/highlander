package utils.db

import scala.concurrent.Future

import cats.Functor
import cats.data.{Validated, Xor}
import failures._
import slick.dbio._
import utils.aliases.EC

// Replace failure with SomethingWentWrong
object * extends BaseStar {

  def <~[A](v: DBIO[Failures Xor A])(implicit ec: EC, f: Functor[DBIO]): DbResultT[A] =
    DbResultT(v).leftMap(SomethingWentWrong(_).single)

  def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
    v.fold(fs ⇒ DbResultT.failure(SomethingWentWrong(fs)), DbResultT.good(_))

  def <~[A](v: Future[Failures Xor A]): DbResultT[A] =
    DbResultT(DBIO.from(v))

  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
    <~(v.toXor)

  def <~[A](v: DbResultT[A])(implicit f: Functor[DBIO]): DbResultT[A] =
    v.leftMap(SomethingWentWrong(_).single)
}
