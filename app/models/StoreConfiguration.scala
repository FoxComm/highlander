package models

import services.PaymentGateway
import utils.ModelWithIdParameter

//  This is a super quick placeholder for store configuration.  We will want to blow this out later.
// TODO: Create full configuration data model
final case class StoreConfiguration(id: Int, storeId: Int, PaymentGateway: PaymentGateway) extends
  ModelWithIdParameter[StoreConfiguration]

