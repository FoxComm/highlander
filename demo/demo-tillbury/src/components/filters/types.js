// @flow

export type Filter = {
  key: string,
  values: Array<FilterValue>,
};

export type FilterValue = {
  count: number,
  label: string,
  value: any,
  selected: boolean,
};

export type FilterGroupProps = {
  children?: Element<FilterTypeProps>|Array<Element<FilterTypeProps>>,
  label: string,
  term: string,
  values?: Array<FilterValue>,
  onClearFacet?: Function,
  onSelectFacet?: Function,
};

export type FilterTypeProps = {
  onSelectFacet?: Function,
  term?: string,
  values?: Array<FilterValue>,
};
