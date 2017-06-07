package foxcomm.search

import com.twitter.util.{Future, Promise}
import scala.concurrent.{ExecutionContext, Future => ScalaFuture}
import scala.util.{Failure, Success}


package object api {
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
