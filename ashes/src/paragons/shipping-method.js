/* @flow */

export type ShippingMethod = {
  id: number,
  adminDisplayName: string,
  storefrontDisplayName: string,
  code: string,
  price: number,
  isActive: bool,
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
