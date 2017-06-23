package phoenix

import com.typesafe.scalalogging.Logger
import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

package object utils {
  implicit class RichFuture[A](val future: Future[A]) extends AnyVal {
    def timeoutAfter(timeout: FiniteDuration, logger: Logger)(
        onTimeout: ⇒ A)(implicit ec: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] = {
      val timeoutFuture = Promise[A]()
      val scheduledTimeout = scheduler.schedule(new Runnable {
        def run(): Unit =
          timeoutFuture.trySuccess(onTimeout)
      }, timeout.length, timeout.unit)
      Future.firstCompletedOf(List(future, timeoutFuture.future)).andThen {
        case _ ⇒
          val cancelled = scheduledTimeout.cancel(true)
          if (!cancelled && timeoutFuture.isCompleted) {
            future.onComplete {
              case Success(v) ⇒
                logger.info(s"Call to Stripe has timeouted and then succeeded: $v")
              case Failure(th) ⇒
                logger.error("Call to Stripe has timeouted and then failed", th)
            }
          }
      }
    }
  }
}
