package utils.db

import scala.concurrent.Future

import cats.Functor
import cats.data.{Validated, Xor}
import failures._
import slick.dbio.DBIO
import utils.aliases.EC

// Return explicit failure to client
object *! extends BaseStar {

  def <~[A](v: DBIO[Xor[Failures, A]])(implicit ec: EC, f: Functor[DBIO]): DbResultT[A] =
    DbResultT(v)

  def <~[A](v: Xor[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromXor(v)

  def <~[A](v: Future[Xor[Failures, A]]): DbResultT[A] =
    DbResultT(DBIO.from(v))

  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromXor(v.toXor)

  def <~[A](v: DbResultT[A])(implicit f: Functor[DBIO]): DbResultT[A] =
    v
}
