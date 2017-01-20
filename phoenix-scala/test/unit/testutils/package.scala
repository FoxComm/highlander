import utils.aliases._

package object testutils {
  def originalSourceClue(implicit line: SL, file: SF) =
    s"""\n(Original source: ${file.value.split("/").last}:${line.value})"""
}
