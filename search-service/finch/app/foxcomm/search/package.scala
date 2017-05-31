package foxcomm

import com.sksamuel.elastic4s.IndexAndTypes
import com.twitter.concurrent.AsyncStream
import com.twitter.util.Promise
import io.finch.DecodePath
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}
import org.reactivestreams._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

package object search {
  implicit class RichPublisher[A](val publisher: Publisher[A]) extends AnyVal {
    def toAsyncStream(implicit s: Scheduler): AsyncStream[A] = {
      val result = Promise[AsyncStream[A]]()
      Observable
        .fromReactivePublisher(publisher)
        .consumeWith(Consumer.foldLeft(AsyncStream.empty[A])((s, e) =>
          AsyncStream.mk(e, s)))
        .runAsync
        .onComplete {
          case Success(stream) => result.setValue(stream)
          case Failure(th) => result.setException(th)
        }
      AsyncStream.fromFuture(result).flatten
    }
  }

  implicit val decodeIndexAndTypes: DecodePath[IndexAndTypes] = DecodePath
    .instance(
      path =>
        try Some(IndexAndTypes(path))
        catch { case NonFatal(_) => None })
}
