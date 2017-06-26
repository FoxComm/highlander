package phoenix.libs

import cats.data.EitherT
import cats.implicits._
import org.json4s.DefaultFormats
import phoenix.utils.aliases._

import scala.concurrent.Future

package object oauth {

  implicit val formats = DefaultFormats

  private[oauth] def eitherTryFuture[A](f: ⇒ Future[A])(implicit ec: EC): EitherT[Future, Throwable, A] =
    Either
      .catchNonFatal(f)
      .leftMap(Future.successful)
      .fold(EitherT.left[Future, Throwable, A], EitherT.right[Future, Throwable, A])

  implicit class EnrichedMap[K, V](val m: collection.immutable.Map[K, V]) extends AnyVal {
    def +?(k: K, v: Option[V]): collection.immutable.Map[K, V] =
      v.fold(m) { b ⇒
        m + (k → b)
      }
  }
}
