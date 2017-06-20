
export type Context = {
  setParams: (params: Object) => any;
  setType: (type: string) => any;
  type?: string;
}

export type ItemDesc = {
  type?: string;
  name?: string;
  value?: number,
  widget?: string;
  template?: (props: Object) => Element<*>;
}


export type DiscountRow = Array<ItemDesc>;

export type DescriptionType = {
  type: string;
  title: string;
  content?: Array<DiscountRow>;
  default?: mixed,
}

