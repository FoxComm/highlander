package utils.db

// Ignore DbResultT failure. Convert Left to Option[Right]
object *? {
  // TODO @anna define a Star that folds DbResultT[A] into DbResultT[Option[A]]
}
