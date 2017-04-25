package libs

import cats.data.EitherT
import cats.implicits._
import io.circe.Decoder
import scala.concurrent.Future
import utils.aliases._

package object oauth {
  private[oauth] def eitherTryFuture[A](f: ⇒ Future[Decoder.Result[A]])(
      implicit ec: EC): EitherT[Future, Throwable, A] = {
    Either
      .catchNonFatal(f)
      .leftMap(Future.successful)
      .fold(EitherT.left[Future, Throwable, A], EitherT[Future, Throwable, A])
  }

  implicit class EnrichedMap[K, V](val m: collection.immutable.Map[K, V]) extends AnyVal {
    def +?(k: K, v: Option[V]): collection.immutable.Map[K, V] = {
      v.fold(m) { b ⇒
        m + (k → b)
      }
    }
  }
}
