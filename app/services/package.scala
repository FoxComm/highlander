import collection.immutable

package object services {
  type Failures = immutable.Seq[Failure]
  def Failures(failures: Failure*): Failures = immutable.Seq[Failure](failures: _*)
}
