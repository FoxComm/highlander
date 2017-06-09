package foxcomm.search

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import fs2.{io => _, _}
import fs2.interop.reactivestreams._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import scala.concurrent.ExecutionContext

object Http4sAPI extends StreamApp {
  object Size extends QueryParamDecoderMatcher[Int]("size")
  object From extends OptionalQueryParamDecoderMatcher[Int]("from")

  def service(searchService: SearchService)(implicit ec: ExecutionContext,
                                            strategy: Strategy,
                                            system: ActorSystem) =
    HttpService {
      case req @ POST -> Root / "search" / searchIndex / searchType :? Size(
            size) +& From(from) =>
        for {
          searchQuery <- req.as(jsonOf[SearchQuery])
          searchResult <- Ok(
            searchService
              .searchFor(searchIndex / searchType,
                         searchQuery,
                         searchSize = size,
                         searchFrom = from)
              .map(_.asJson))
        } yield searchResult
      case req @ POST -> Root / "streamSearch" / searchIndex / searchType =>
        for {
          searchQuery <- req.as(jsonOf[SearchQuery])
          searchResult <- Ok(
            searchService
              .streamSearchFor(searchIndex / searchType, searchQuery)
              .toStream[Task]
          )
        } yield searchResult
    }

  def stream(args: List[String]): Stream[Task, Nothing] = {
    implicit val system: ActorSystem = ActorSystem("Http4sAPI")
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val strategy: Strategy = Strategy.fromExecutionContext(ec)

    val config = AppConfig.load()
    val svc = SearchService.fromConfig(config)

    BlazeBuilder
      .bindHttp(config.http.port, config.http.interface)
      .mountService(service(svc), "/")
      .serve
  }
}
