import type { Address } from 'types/address';

export type CheckoutBlockProps = {
  isEditing: boolean,
  inProgress?: boolean,
  collapsed: boolean,
  continueAction: Function,
  editAction: Function,
};

export type CreditCardType = {
  id: number,
  brand: string,
  lastFour: string,
  expMonth: number,
  expYear: number,
};

export type BillingData = {
  id: number,
  holderName: string,
  brand: string,
  lastFour: string,
  expMonth: number,
  expYear: number,
  billingAddress?: Object,
  address?: Address,
};

type CardActions = {
  setBillingData: Function,
  resetBillingData: Function,
  selectCreditCard: Function,
  updateCreditCard: Function,
  addCreditCard: Function,
  loadBillingData: Function,
  deleteCreditCard: Function,
  chooseCreditCard: Function,
};

type AddressActions = {
  fetchAddresses: Function,
  updateAddress: Function,
  saveShippingAddress: () => PromiseType,
  setDefaultAddress: () => PromiseType,
};

export type CheckoutActions = CardActions & AddressActions & {
  saveCouponCode: Function,
  saveGiftCard: Function,
  fetchShippingMethods: Function,
  checkout: () => PromiseType,
  saveShippingMethod: () => PromiseType,
};
