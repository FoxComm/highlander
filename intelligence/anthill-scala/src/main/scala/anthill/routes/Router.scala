package anthill.routes

object Router {
  val routes = (
    "public" :: (
      Ping.ping :+:
        ProdProd.prodProd :+:
        CustProd.custProd
    )
  ) :+: (
    "private" :: Train.purchaseEvent
  )
}
