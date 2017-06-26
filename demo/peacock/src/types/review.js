type GenericAttributeElement = {
  t: 'string',
  v: string,
};

type Attributes = {
  imageUrl: GenericAttributeElement,
  productName: GenericAttributeElement,
  status: {
    t: 'string',
    v: 'pending' | 'submitted',
  },
  body?: GenericAttributeElement,
  title?: GenericAttributeElement,
};

export type Review = {
  archivedAt: ?string,
  attributes: Attributes,
  createdAt: string,
  id: number,
  rating: ?number, // not sure here
  scope: string,
  sku: string,
  title: ?string,
  updatedAt: string,
  userId: string,
  userName: string,
};
