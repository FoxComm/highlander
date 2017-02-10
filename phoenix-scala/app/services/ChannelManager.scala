package services

import cats.implicits._
import payloads.ChannelPayloads._
import utils.aliases._
import utils.db._

object ChannelManager {
  def createChannel(
      payload: CreateChannelPayload)(implicit ec: EC, db: DB, oc: OC, au: AU): DbResultT[Unit] = {
    DbResultT.unit
  }
}
