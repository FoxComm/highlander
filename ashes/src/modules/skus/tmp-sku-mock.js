
const number = 1;
const string = 'test';
const Currency = {
  currency: 'USD',
  value: 10000,
};

const Dimension = {
  value: 100,
  units: 'cm',
};

const QuantityLevel = {
  isEnabled: false,
  level: number,
};

const boolean = false;

const r = {
  id: number,
  code: string,
  upc: string,
  title: string,
  unitCost: Currency,
  taxClass: string,
  requiresShipping: boolean,
  shippingClass: string,
  isReturnable: boolean,
  returnWindow: Dimension,
  width: Dimension,
  height: Dimension,
  weight: Dimension,
  length: Dimension,
  requiresInventoryTracking: boolean,
  inventoryWarningLevel: QuantityLevel,
  maximumQuantityInCart: QuantityLevel,
  minimumQuantityInCart: QuantityLevel,
  allowBackorder: boolean,
  allowPreorder: boolean,
  requiresLotTracking: boolean,
  lotExpirationThreshold: Dimension,
  lotExpirationWarningThreshold: Dimension,
};

module.exports = r;
