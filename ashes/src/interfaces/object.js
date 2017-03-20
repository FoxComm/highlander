declare type Attribute = { t: string, v: any };
declare type Attributes = {
  [name: string]: Attribute;
};

declare type Context = {
  name: string,
  attributes?: {
    lang: string,
    modality: string,
  },
}

declare type ObjectPageLayout = {
  content: Array<Object>,
  aside: Array<Object>,
};

declare type Fields = {
  canAddProperty: boolean,
  value: Array<string>,
  includeRest: boolean,
  omit: Array<string>,
}

declare type NodeDesc = {
  type: string,
  title?: string,
  fields?: Fields,
  renderer?: string,
  content?: Array<NodeDesc>,
}

declare type ObjectView = {
  attributes: Attributes,
  context: Context;
}

declare type ObjectSchema = {
  type: string,
  title: string,
  properties: {
    attributes: { [key:string]: any },
    description: string,
  },
};

declare type ObjectActions<T> = {
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

declare type ObjectPageProps<T, U> = {
  layout: ObjectPageLayout,
  schema: ?ObjectSchema,
  actions: ObjectActions<T>,
  children?: Element,
  context?: string,
  identifier: string,
  navLinks: NavLinks<U>,
  object: ?T,
  objectType: string,
  internalObjectType?: string, // field for cases when UI type (breadcrumbs and lots of block titles are based on it) differs from api object type (e.g. taxon -> value)
  identifierFieldName: string,
  originalObject: ?T,
  fetchState: AsyncState,
  saveState: AsyncState,
  archiveState: AsyncState,
  onUpdateObject: (object: T) => void,
};

declare type ObjectPageChildProps<T> = {
  layout: ObjectPageLayout,
  schema: ObjectSchema,
  object: T,
  objectType: string,
  internalObjectType?: string,
  onUpdateObject: (object: T) => void,
};
