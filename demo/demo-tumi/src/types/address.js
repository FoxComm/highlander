
type State = {
  name: string,
};

type Country = {
  alpha3: string,
  name?: string,
};

type Region = {
  id: number,
  countryId: number,
  name: string,
};

export type Address = {
  id?: number,
  region?: Region,
  country?: Country,
  state?: State,
  city?: string,
  name: string,
  address1: string,
  address2?: string,
  zip: string|number,
  isDefault?: boolean,
  phoneNumber?: string,
  isDeleted?: boolean,
};
