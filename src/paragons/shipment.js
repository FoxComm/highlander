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

export type Shipment = {
  id: number;
  shippingMethodId: number;
  referenceNumber: string;
  state: string;
  shipmentDate: ?string;
  deliveredDate: ?string;
  estimatedArrival: ?string;
  address: Object;
  lineItems: Array<ShipmentLineItem>;
  trackingNumber: ?string;
};

export type ShipmentLineItem = {
  id: number;
  referenceNumber: string;
  sku: string;
  name: string;
  price: number;
  imagePath: string;
  state: string;
  quantity: number;
};

export type UnshippedLineItem = ShipmentLineItem & {
  reason: string;
};

export type Carrier = {
  id: number;
  name: string;
  trackingTemplate: string;
};

export type ShippingMethod = {
  id: number;
  carrierId: ?number;
  carrier: ?Carrier;
  name: string;
};
