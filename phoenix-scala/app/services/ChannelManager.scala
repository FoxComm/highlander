package services

import cats.implicits._
import models.account._
import models.channel._
import models.objects._
import org.json4s._
import payloads.ChannelPayloads._
import responses.ChannelResponse
import responses.ObjectResponses.ObjectContextResponse
import utils.aliases._
import utils.db._

object ChannelManager {
  def createChannel(payload: CreateChannelPayload)(implicit ec: EC,
                                                   db: DB,
                                                   oc: OC,
                                                   au: AU): DbResultT[ChannelResponse.Root] =
    for {
      scope   ← * <~ Scope.resolveOverride(payload.scope)
      context ← * <~ findOrCreateContext(payload)
      channel ← * <~ Channels.create(Channel.build(payload, context.id, scope))
    } yield ChannelResponse.build(channel, context, context)

  private def findOrCreateContext(payload: CreateChannelPayload)(implicit ec: EC, db: DB, oc: OC) =
    payload.contextId match {
      case Some(contextId) ⇒
        for {
          context ← * <~ ObjectContexts.mustFindById404(contextId)
        } yield context
      case None ⇒
        val newContext = ObjectContext(name = payload.name, attributes = JNothing)
        for {
          context ← * <~ ObjectContexts.create(newContext)
        } yield context
    }
}
