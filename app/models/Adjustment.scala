package models

import utils.ModelWithIdParameter

final case class Adjustment(id: Int, amount: Int, sourceId: Int, sourceType: String, reason: String)
  extends ModelWithIdParameter

