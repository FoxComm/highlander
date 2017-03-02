
export type Attribute = { t: string, v: any };
export type Attributes = {
  [name: string]: Attribute;
};

export type Context = {
  name: string,
  attributes?: {
    lang: string,
    modality: string,
  },
}

export type ObjectView = {
  attributes: Attributes,
  context: Context;
}

export type ObjectSchema = {
  type: string,
  title: string,
  properties: {
    attributes: { [key:string]: any },
    description: string,
  },
};

export type ObjectActions<T> = {
  newObject: () => void,
  duplicate: () => void,
  reset: () => void,
  fetch: (id: string, context?: string) => void,
  create: (object: T, context?: string) => void,
  update: (object: T, context?: string) => void,
  archive: (object: T, context?: string) => void,
  cancel: () => void,
  getTitle: (object: T) => string,
  transition: () => void,
};

export type ObjectProps<T, U> = {
  actions: ObjectActions<T>,
  children?: Element<*>|Array<Element<*>>,
  context?: string,
  identifier: string,
  isFetching: boolean,
  fetchError: ?Object,
  navLinks: NavLinks<U>,
  object: ?T,
  objectType: string,
  originalObject: ?T,
};
