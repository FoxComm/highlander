declare type OptionValue = {
  name: string,
  swatch: ?string,
  image: ?string,
  skuCodes: Array<string>,
};

declare type Option = {
  attributes?: {
    name: { t: string, v: string },
    type?: { t: string, v: string },
  },
  values: Array<OptionValue>,
};

declare type Product = ObjectView & {
  id?: number,
  productId: ?number,
  skus: Array<Sku>,
  variants: Array<Option>,
  albums: Array<*>,
};
