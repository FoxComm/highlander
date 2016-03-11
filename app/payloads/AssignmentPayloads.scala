package payloads

final case class AssignmentPayload(assigneeIds: Seq[Int])

final case class BulkAssignmentPayload[K](entityIds: Seq[K], assigneeId: Int)
