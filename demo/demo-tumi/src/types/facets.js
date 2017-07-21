
export type FacetElementProps = {
  reactKey: string,
  key: string,
  facet: string,
  value: string,
  label: string,
  checked?: boolean,
  click: (facet: string, value: string|Object, checked: boolean) => void,
};

export type FacetValue = {
  label: string,
  +value: Object|string,
  count?: number,
  selected?: boolean,
}

export type Facet = {
  key?: string,
  name: string,
  kind: 'color' | 'circle' | 'checkbox',
  values: Array<FacetValue>,
}

