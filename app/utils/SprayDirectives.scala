package utils

import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import services.Result
import utils.Http._

import scala.concurrent.{ExecutionContext, Future}

object SprayDirectives {

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
