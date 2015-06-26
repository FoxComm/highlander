package utils

// Yax --> :trollface: for you.  TODO: Name this better
object StringStuff {

  def lowerCaseFirstLetter(s:String) = s(0).toLower + s.substring(1)

  def upperCaseFirstLetter(s:String) = s(0).toUpper + s.substring(1)
}
