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
  reset: () => void,
  close: () => void,
  duplicate: () => void,
  fetch: (id: string, context?: string) => Promise<*>,
  create: (object: T, context?: string) => Promise<*>,
  update: (object: T, context?: string) => Promise<*>,
  archive: (object: T, context?: string) => Promise<*>,
  getTitle: (object: T) => string,
  transition: (id: number|string) => void,
  clearFetchErrors: () => void,
  clearArchiveErrors: () => void,
};

export type ObjectProps<T, U> = {
  actions: ObjectActions<T>,
  children?: Element<*>|Array<Element<*>>,
  context?: string,
  identifier: string,
  navLinks: NavLinks<U>,
  object: ?T,
  objectType: string,
  identifierFieldName: string,
  originalObject: ?T,
  fetchState: AsyncState,
  saveState: AsyncState,
  archiveState: AsyncState,
};
