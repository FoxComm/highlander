declare type Sku = {
  code?: string,
  feCode?: string,
  attributes: Attributes,
  id: any,
  context: {
    attributes?: Object,
    name: string,
  },
  albums: Array<*>,
};

declare type SkuSearchItem = {
  id: number;
  image: string|null,
  context: string,
  skuCode: string,
  title: string,
  salePrice: string,
  salePriceCurrency: string,
  retailPrice: string,
  retailPriceCurrency: string,
};

declare type SkuSearch = Array<SkuSearchItem>;
