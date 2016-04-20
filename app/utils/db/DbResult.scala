package utils.db

import scala.concurrent.Future

import cats.data.Xor
import failures.{Failure, _}
import services.Result
import slick.dbio.DBIO
import utils.aliases._

object DbResult {

  val unit: DbResult[Unit] = DBIO.successful(Xor.right(Unit))

  def none[A]: DbResult[Option[A]] = good(Option.empty[A])

  def fromXor[A](xor: Failures Xor A): DbResult[A] = xor.fold(failures, good)

  def good[A](v: A): DbResult[A] = lift(Xor.right(v))

  def fromDbio[A](dbio: DBIO[A])(implicit ec: EC): DbResult[A] = dbio.map(Xor.right)

  def fromFuture[A](future: Future[A])(implicit ec: EC): DbResult[A] = fromDbio(liftFuture(future))

  def failure[A](failure: Failure): DbResult[A] = liftFuture(Result.failure(failure))

  def failures[A](failures: Failures): DbResult[A] = liftFuture(Result.failures(failures))
}
