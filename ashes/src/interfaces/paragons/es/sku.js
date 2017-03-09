
export type TStockLocation = {
  id: number,
  name: string,
}

export type TStockItem = {
  id: number,
  sku: string,
  defaultUnitCost: number,
}

export type TStockCounts = {
  onHand: number,
  onHold: number,
  reserved: number,
  shipped: number,
  afs: number,
  afsCost: number,
}

export type TSearchViewSku = TStockCounts & {
  sku: string,
  stockItem: TStockItem,
  stockLocation: TStockLocation,
  type: string,
  createdAt: string,
  updatedAt: string,
  deletedAt: string,
  scope: string,
}
