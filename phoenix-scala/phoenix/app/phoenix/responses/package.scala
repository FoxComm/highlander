import phoenix.responses.TheResponse

package object responses {
  type BatchResponse[T] = TheResponse[Seq[T]]
}
