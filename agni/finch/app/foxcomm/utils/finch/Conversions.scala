package foxcomm.utils.finch

import scala.language.implicitConversions
import com.twitter.util.{Return, Throw, Future ⇒ TFuture, Promise ⇒ TPromise}
import scala.concurrent.{ExecutionContext, Future ⇒ SFuture, Promise ⇒ SPromise}
import scala.util.{Failure, Success}
import Conversions._
import monix.eval.{Callback, Task}
import monix.execution.Scheduler

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
trait Conversions {
  implicit def toRichTask[A](task: Task[A]): RichTask[A] = new RichTask(task)

  implicit def toRichSFuture[A](future: SFuture[A]): RichSFuture[A] = new RichSFuture(future)

  implicit def toRichTFuture[A](future: TFuture[A]): RichTFuture[A] = new RichTFuture(future)
}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object Conversions {
  implicit class RichTask[A](val task: Task[A]) extends AnyVal {
    def toTwitterFuture(implicit s: Scheduler): TFuture[A] = {
      val result = TPromise[A]()
      task.runAsync(new Callback[A] {
        def onError(ex: Throwable): Unit = result.setException(ex)

        def onSuccess(value: A): Unit = result.setValue(value)
      })
      result
    }
  }

  implicit class RichSFuture[A](val future: SFuture[A]) extends AnyVal {
    def toTwitterFuture(implicit ec: ExecutionContext): TFuture[A] = {
      val result = TPromise[A]()
      future.onComplete {
        case Success(a)  ⇒ result.setValue(a)
        case Failure(th) ⇒ result.setException(th)
      }
      result
    }
  }

  implicit class RichTFuture[A](val future: TFuture[A]) extends AnyVal {
    def toScalaFuture: SFuture[A] = {
      val result = SPromise[A]()
      future.respond {
        case Return(a) ⇒ result.success(a)
        case Throw(th) ⇒ result.failure(th)
      }
      result.future
    }
  }
}
