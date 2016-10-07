package object responses {
  type BatchResponse[T] = TheResponse[Seq[T]]
}
