package responses

import io.circe.syntax._
import java.time.Instant
import models.Assignment
import models.account.User
import responses.UserResponse.{build ⇒ buildUser}
import utils.aliases._
import utils.json.codecs._

object AssignmentResponse {

  case class Root(assignee: UserResponse.Root,
                  assignmentType: Assignment.AssignmentType,
                  createdAt: Instant)
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(assignment: Assignment, admin: User): Root = {

    Root(assignee = buildUser(admin),
         assignmentType = assignment.assignmentType,
         createdAt = assignment.createdAt)
  }
}
