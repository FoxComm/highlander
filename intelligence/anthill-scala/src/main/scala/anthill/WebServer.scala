package anthill

import anthill.routes.Router
import com.twitter.finagle.Http
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch.circe._

object WebServer {
  def main(args: Array[String]): Unit = {
    println("listening on localhost:8880")
    Await.ready(Http.server.serve(":8880", Router.routes.toService))
  }
}
