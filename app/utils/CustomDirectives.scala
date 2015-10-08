package utils

import akka.http.scaladsl.server.{Directive1, StandardRoute}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.Directives._

import services.Result
import utils.Http._

import scala.concurrent.{ExecutionContext, Future}

object CustomDirectives {

  case class Sort(sortField: String, asc: Boolean = true)
  case class Start(startElement: Int)
  case class Page(pageNo: Int, pageSize: Int)

  def sort: Directive1[Option[Sort]] =
    parameter('sortField.as[String].?).map { field: Option[String] ⇒
      field map { f ⇒
        if (f.startsWith("-")) Sort(f.drop(1), asc = false)
        else Sort(f)
      }
    }

  def start: Directive1[Option[Start]] =
    parameter('startElement.as[Int].?).map { startElement: Option[Int] ⇒
      startElement.map(Start)
    }

  def page: Directive1[Option[Page]] =
    parameters(('pageNo.as[Int].?, 'pageSize.as[Int].?)).tmap { case (pageNoOpt: Option[Int], pageSizeOpt:
      Option[Int]) ⇒
      for {
        pageNo   ← pageNoOpt
        pageSize ← pageSizeOpt
      } yield Page(pageNo, pageSize)
    }

  def sortAndStart = sort & start

  def sortAndPage = sort & page

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
