package foxcomm.utils.finch

import cats.implicits._
import com.twitter.finagle.http.Status
import com.twitter.util.Future
import io.circe.Decoder
import io.circe.parser.decode
import io.finch._
import pdi.jwt.Jwt
import scala.util.{Failure, Success}

trait JWT {
  def jwtClaims[A: Decoder]: Endpoint[A] =
    (cookieOption("JWT").map(_.map(_.value)) coproduct headerOption("JWT")).mapOutputAsync {
      case Some(rawToken) ⇒
        Future {
          Jwt.decode(rawToken).flatMap(decode[A](_).toTry) match {
            case Success(v) ⇒ Output.payload(v)
            case Failure(th) ⇒
              Output.failure(new RuntimeException("Error during JWT verification", th), Status.Unauthorized)
          }
        }
      case None ⇒ Future.value(Output.empty(Status.Unauthorized))
    }
}
