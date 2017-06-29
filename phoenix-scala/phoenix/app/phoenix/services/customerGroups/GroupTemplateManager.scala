package phoenix.services.customerGroups

import phoenix.models.account.Scope
import phoenix.models.customer._
import phoenix.responses.GroupResponses.GroupTemplateResponse
import phoenix.utils.aliases._
import core.db._
import core.db.ExPostgresDriver.api._

object GroupTemplateManager {

  def getAll()(implicit ec: EC, db: DB, au: AU): DbResultT[Seq[GroupTemplateResponse]] =
    for {
      scope         ← * <~ Scope.current
      usedTemplates ← * <~ GroupTemplateInstances.findByScope(scope).result
      usedTemplateIds = usedTemplates.map(_.groupTemplateId)
      templates ← * <~ CustomerGroupTemplates.result
    } yield templates.filterNot(t ⇒ usedTemplateIds.contains(t.id)).map(GroupTemplateResponse.build)

}
