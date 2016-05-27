/* @flow */

export type Attributes = { [key:string]: any };

export type SkuForm = {
  code: ?string,
  attributes: Attributes,
  createdAt: ?string,
};

export type SkuShadow = {
  code: ?string,
  attributes: Attributes,
  createdAt: ?string,
};

export type FullSku = {
  id: ?number,
  code: string,
  form: SkuForm,
  shadow: SkuShadow,
};

export function generateSkuCode(): string {
  return Math.random().toString(36).substring(7).toUpperCase();
}

export function createEmptySku(): FullSku {
  const pseudoRandomCode = generateSkuCode();

  const form:SkuForm = {
    code: pseudoRandomCode,
    attributes: {
      title: '',
      upc: '',
      description: '',
      retailPrice: {
        value: 0,
        currency: 'USD'
      },
      salePrice: {
        value: 0,
        currency: 'USD'
      }
    },
    createdAt: null,
  };

  const shadow: SkuShadow = {
    code: pseudoRandomCode,
    attributes: {
      title: { type: 'string', ref: 'title' },
      upc: { type: 'string', ref: 'title' },
      description: { type: 'richText', ref: 'title' },
      retailPrice: { type: 'price', ref: 'retailPrice' },
      salePrice: { type: 'price', ref: 'salePrice' },
      unitCost: { type: 'price', ref: 'unitCost' },
    },
    createdAt: null,
  };

  return {
    id: null,
    code: pseudoRandomCode,
    form,
    shadow,
  };
}
