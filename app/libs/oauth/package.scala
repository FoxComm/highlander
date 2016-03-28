package libs

import scala.concurrent.Future

import cats.data.{Xor, XorT}
import cats.implicits._
import org.json4s.DefaultFormats
import utils.aliases._

package object oauth {

  implicit val formats = DefaultFormats

  private[oauth] def xorTryFuture[A](f: ⇒ Future[A])(implicit ec: EC): XorT[Future, Throwable, A] = {
    Xor.catchNonFatal(f).leftMap(Future.successful)
      .fold(XorT.left[Future, Throwable, A], XorT.right[Future, Throwable, A])
  }

  implicit class EnrichedMap[K, V](val m: collection.immutable.Map[K,V]) extends AnyVal {
    def +?(k:K, v: Option[V]): collection.immutable.Map[K,V] =  {
      v.fold(m) { b ⇒ m + (k → b) }
    }
  }
}
