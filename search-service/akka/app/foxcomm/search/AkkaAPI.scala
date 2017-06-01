package foxcomm.search

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.sksamuel.elastic4s.ElasticDsl._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe.generic.auto._
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object AkkaAPI {
  def routes(searchService: SearchService)(implicit ec: ExecutionContext,
                                           system: ActorSystem): Route =
    (post & pathPrefix("search") & pathPrefix(Segment) & path(Segment) & entity(
      as[SearchQuery])) { (searchIndex, searchType, searchQuery) =>
      parameters('size.as[Int], 'from.as[Int].?) { (size, from) =>
        complete(
          searchService.searchFor(searchIndex / searchType,
                                  searchQuery,
                                  searchSize = size,
                                  searchFrom = from))
      }
    } ~ (post & pathPrefix("streamSearch") & pathPrefix(Segment) & path(
      Segment) & entity(as[SearchQuery])) {
      (searchIndex, searchType, searchQuery) =>
        complete {
          HttpEntity(
            contentType = ContentTypes.`application/json`,
            data = Source
              .fromPublisher(
                searchService.streamSearchFor(searchIndex / searchType,
                                              searchQuery))
              .map(_.sourceRef.toBytes)
              .map(ByteString(_))
          )
        }
    }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("AkkaAPI")
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config = AppConfig.load()
    val svc = SearchService.fromConfig(config)

    Await.ready(Http().bindAndHandle(
                  handler = routes(svc),
                  interface = config.http.interface,
                  port = config.http.port
                ),
                Duration.Inf)
  }
}
