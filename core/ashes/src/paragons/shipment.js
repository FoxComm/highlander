/* @flow */

export const itemStates = {
  pending: 'pending',
  delivered: 'delivered',
  cancelled: 'cancelled'
};

export const itemStateTitles = {
  [itemStates.pending]: 'Pending',
  [itemStates.delivered]: 'Delivered',
  [itemStates.cancelled]: 'Cancelled',
};

export const itemReasons = {
  outOfStock: 'outOfStock',
};

export const itemReasonsTitles = {
  [itemReasons.outOfStock]: 'Out Of Stock',
};

export type TShipment = {
  id: number;
  shippingMethodId: number;
  shippingMethod: TShippingMethod;
  referenceNumber: string;
  state: string;
  shipmentDate: ?string;
  deliveredDate: ?string;
  estimatedArrival: ?string;
  address: Object;
  lineItems: Array<TShipmentLineItem>;
  trackingNumber: ?string;
};

export type TShipmentLineItem = {
  id: number;
  referenceNumber: string;
  sku: string;
  name: string;
  price: number;
  imagePath: string;
  state: string;
  quantity: number;
};

//TODO - reasons are to be implemented in middlewarehouse
export type TUnshippedLineItem = TShipmentLineItem & {
  reason?: string;
};

export type TCarrier = {
  id: number;
  name: string;
  trackingTemplate: string;
};

export type TShippingMethod = {
  id: number;
  carrier: TCarrier;
  name: string;
};
