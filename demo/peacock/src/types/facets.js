
export type FacetElementProps = {
  facet: string,
  value: string,
  label: string,
  checked?: boolean,
  click: Function,
};

export type FacetValue = {
  label: string,
  value: Object|string,
  count: number,
}

export type Facet = {
  key: string,
  name: string,
  kind: string,
  values: Array<FacetValue>,
}

