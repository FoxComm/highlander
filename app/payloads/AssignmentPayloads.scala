package payloads

case class AssignmentPayload(assignees: Seq[Int])

case class BulkAssignmentPayload[K](entityIds: Seq[K], storeAdminId: Int)
