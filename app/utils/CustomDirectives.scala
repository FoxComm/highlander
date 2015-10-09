package utils

import akka.http.scaladsl.server.{Directive1, StandardRoute}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.Directives._

import services.Result
import utils.Http._

import scala.concurrent.{ExecutionContext, Future}

object CustomDirectives {

  final case class Sort(sortColumn: String, asc: Boolean = true)
  final case class SortAndPage(
    pageNo: Option[Int],
    pageSize: Option[Int],
    sort: Option[Sort])

  def sortAndPage: Directive1[SortAndPage] =
    parameters(('pageNo.as[Int].?, 'pageSize.as[Int].?, 'sortColumn.as[String].?)).tmap {
      case (pageNoOpt: Option[Int], pageSizeOpt: Option[Int], sortColumnOpt: Option[String]) ⇒
        val sort = sortColumnOpt.map { f ⇒
          if (f.startsWith("-")) Sort(f.drop(1), asc = false)
          else Sort(f)
        }
        SortAndPage(pageNoOpt, pageSizeOpt, sort)
    }

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
