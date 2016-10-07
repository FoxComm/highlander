
type State = {
  name: string;
}

type Country = {
  alpha3: string;
  name?: string;
}

export type Address = {
  name: string;
  address1: string;
  address2?: string;
  state: State;
  country: Country;
  zip: string|number;
  phone?: string;
};
