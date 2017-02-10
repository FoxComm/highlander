package services

import java.time.Instant

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
  def findById(
      channelId: Int)(implicit ec: EC, db: DB, oc: OC, au: AU): DbResultT[ChannelResponse.Root] =
    for {
      channel        ← * <~ Channels.mustFindActive404(channelId)
      defaultContext ← * <~ ObjectContexts.mustFindById404(channel.defaultContextId)
      draftContext   ← * <~ ObjectContexts.mustFindById404(channel.draftContextId)
    } yield ChannelResponse.build(channel, defaultContext, draftContext)

  def createChannel(payload: CreateChannelPayload)(implicit ec: EC,
                                                   db: DB,
                                                   oc: OC,
                                                   au: AU): DbResultT[ChannelResponse.Root] =
    for {
      scope          ← * <~ Scope.resolveOverride(payload.scope)
      defaultContext ← * <~ findOrCreateContext(payload)
      draftContext ← * <~ ObjectContexts.create(
                        defaultContext.copy(id = 0, name = s"${defaultContext.name}-draft"))
      channel ← * <~ Channels.create(
                   Channel.build(payload, defaultContext.id, draftContext.id, scope))
    } yield ChannelResponse.build(channel, defaultContext, draftContext)

  def updateChannel(channelId: Int, payload: UpdateChannelPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB,
      au: AU): DbResultT[ChannelResponse.Root] =
    for {
      channel        ← * <~ Channels.mustFindActive404(channelId)
      up             ← * <~ Channels.update(channel, channel.copy(name = payload.name))
      defaultContext ← * <~ ObjectContexts.mustFindById404(channel.defaultContextId)
      draftContext   ← * <~ ObjectContexts.mustFindById404(channel.draftContextId)
    } yield ChannelResponse.build(up, defaultContext, draftContext)

  def archiveChannel(channelId: Int)(implicit ec: EC, oc: OC, db: DB, au: AU): DbResultT[Unit] =
    for {
      channel ← * <~ Channels.mustFindActive404(channelId)
      _ ← * <~ Channels
           .update(channel, channel.copy(updatedAt = Instant.now, archivedAt = Instant.now.some))
    } yield {}

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
