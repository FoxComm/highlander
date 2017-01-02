/* @flow */

export type BusinessProfile = {
  legal_entity_name: string,
  bank_account_number: string,
  bank_routing_number: string,
  representative_ssn_trailing_four: string,
  legal_entity_tax_id: string,
  business_founded_day: string,
  business_founded_month: string,
  business_founded_year: string,
  address1: string,
  address2: ?string,
  city: string,
  state: string,
  zip: string, 
}