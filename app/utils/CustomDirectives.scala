package utils

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{Directive1, StandardRoute}

import services.Result
import slick.driver.PostgresDriver.api._
import utils.Http._
import utils.Slick.implicits.ResultWithMetadata

object CustomDirectives {

  val DefaultPageSize = 50

  final case class Sort(sortColumn: String, asc: Boolean = true)
  final case class SortAndPage(
    from: Option[Int] = Some(0),
    size: Option[Int] = Some(DefaultPageSize),
    sortBy: Option[String]) {

    require(from.getOrElse(1) >= 0, "from parameter must be non-negative")
    require(size.getOrElse(1) >  0, "size parameter must be positive")

    def sort: Option[Sort] = sortBy.map { f ⇒
      if (f.startsWith("-")) Sort(f.drop(1), asc = false)
      else Sort(f)
    }
  }

  val EmptySortAndPage: SortAndPage = SortAndPage(None, None, None)

  def sortAndPage: Directive1[SortAndPage] =
    parameters(('from.as[Int].?, 'size.as[Int].?, 'sortBy.as[String].?)).as(SortAndPage)

  def good[A <: AnyRef](a: Future[A])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(render(_)))

  def good[A <: AnyRef](a: A): StandardRoute =
    complete(render(a))

  def goodOrNotFound[A <: AnyRef](a: Future[Option[A]])(implicit ec: ExecutionContext): StandardRoute =
    complete(renderOrNotFound(a))

  def goodOrFailures[A <: AnyRef](a: Result[A])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(renderGoodOrFailures))

  def goodOrFailures[A <: AnyRef](a: ResultWithMetadata[A])
    (implicit db: Database, ec: ExecutionContext): StandardRoute =
    complete(a.asResponseFuture.map(renderGoodOrFailuresWithMetadata))

  def goodOrFailures[A <: AnyRef](a: Future[ResultWithMetadata[A]])
    (implicit db: Database, ec: ExecutionContext): StandardRoute =
    complete(a.flatMap(_.asResponseFuture.map(renderGoodOrFailuresWithMetadata)))

  def nothingOrFailures(a: Result[_])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(renderNothingOrFailures))
}
