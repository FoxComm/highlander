import collection.immutable

package object services {
  type Failures = immutable.Seq[Failure]
  private [services] def Failures(failures: Failure*): Failures = immutable.Seq[Failure](failures: _*)

  implicit class FailuresOps(val underlying: Failure) extends AnyVal {
    def single: Failures = Failures(underlying)
  }
}
