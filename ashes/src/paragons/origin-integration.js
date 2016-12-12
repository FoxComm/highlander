/* @flow */

export type OriginIntegration = {
  id?: number,
  shopify_key: string,
  shopify_password: string,
  shopify_domain: string,
};

export type ProductFeed = {
  name: string,
  url: string,
  format: string,
  schedule: string,
};