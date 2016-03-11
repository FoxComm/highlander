package payloads

final case class AssignmentPayload(assignees: Seq[Int])

final case class BulkAssignmentPayload[K](entityIds: Seq[K], storeAdminId: Int)
