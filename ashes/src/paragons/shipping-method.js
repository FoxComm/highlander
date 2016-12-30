/* @flow */

export type ShippingMethod = {
  id: number,
  carrier: {
    id: number,
    name: string,
    trackingTemplate: string,
  },
  name: string,
  code: string,
  type: string,
  price: {
    currency: string,
    value: number,
  },
};

export type CreatePayload = {
  adminDisplayName: string,
  storefrontDisplayName: string,
  code: string,
  price: number,
};

export type UpdatePayload = {
  adminDisplayName?: string,
  storefrontDisplayName?: string,
  price?: number,
};
