import utils.aliases._

package object util {

  def originalSourceClue(implicit line: SL, file: SF) =
    s"""(Original source: ${file.value.split("/").last}:${line.value})"""

}
