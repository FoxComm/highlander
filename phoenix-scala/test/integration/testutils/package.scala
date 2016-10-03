import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import utils.aliases._

package object testutils {

  def originalSourceClue(implicit line: SL, file: SF) =
    s"""(Original source: ${file.value.split("/").last}:${line.value})"""

  type FoxSuite = Suite with PatienceConfiguration with DbTestSupport
}
