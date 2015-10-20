package utils

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{Directive1, StandardRoute}

import services.Result
import utils.Http._

object CustomDirectives {

  final case class Sort(sortColumn: String, asc: Boolean = true)
  final case class SortAndPage(
    pageNo: Option[Int],
    pageSize: Option[Int],
    sortBy: Option[String]) {

    require(pageNo.getOrElse(1) > 0,   "pageNo parameter must be greater than zero")
    require(pageSize.getOrElse(1) > 0, "pageSize parameter must be greater than zero")

    def sort: Option[Sort] = sortBy.map { f ⇒
      if (f.startsWith("-")) Sort(f.drop(1), asc = false)
      else Sort(f)
    }
  }

  val EmptySortAndPage: SortAndPage = SortAndPage(None, None, None)

  def sortAndPage: Directive1[SortAndPage] =
    parameters(('pageNo.as[Int].?, 'pageSize.as[Int].?, 'sortBy.as[String].?)).as(SortAndPage)

  def good[A <: AnyRef](a: Future[A])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(render(_)))

  def good[A <: AnyRef](a: A): StandardRoute =
    complete(render(a))

  def goodOrNotFound[A <: AnyRef](a: Future[Option[A]])(implicit ec: ExecutionContext): StandardRoute =
    complete(renderOrNotFound(a))

  def goodOrFailures[A <: AnyRef](a: Result[A])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(renderGoodOrFailures))

  def nothingOrFailures(a: Result[_])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(renderNothingOrFailures))
}
