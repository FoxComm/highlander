package foxcomm

import com.twitter.concurrent.AsyncStream
import com.twitter.util.{Future, Promise}
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}
import org.reactivestreams._
import scala.concurrent.{ExecutionContext, Future => ScalaFuture}
import scala.util.{Failure, Success}

package object search {
  implicit class RichPublisher[A](val publisher: Publisher[A]) extends AnyVal {
    def toTwitterAsyncStream(implicit s: Scheduler): AsyncStream[A] = {
      AsyncStream
        .fromFuture(
          Observable
            .fromReactivePublisher(publisher)
            .consumeWith(Consumer.foldLeft(AsyncStream.empty[A])((s, e) =>
              AsyncStream.mk(e, s)))
            .runAsync
            .toTwitterFuture)
        .flatten
    }
  }

  implicit class RichFuture[A](val future: ScalaFuture[A]) extends AnyVal {
    def toTwitterFuture(implicit ec: ExecutionContext): Future[A] = {
      val result = Promise[A]()
      future.onComplete {
        case Success(a) => result.setValue(a)
        case Failure(th) => result.setException(th)
      }
      result
    }
  }
}
