/* @flow */

export type MerchantApplication = {
  id: number,
  state: string,
  reference_number: string,
  name: string,
  email_address: string,
  description: string,
  business_name: ?string,
};

export type BusinessProfile = {
  monthly_sales_volume: string,
  categories: ?Array<string>,
  target_audience: ?Array<string>,
};

export type SocialProfile = {
  twitter_handle: ?string,
}
