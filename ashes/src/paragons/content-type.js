
import { assoc } from 'sprout-data';

let id = 0;
function uniqId() {
  return id++;
}

export function addContentTypeObject(contentType, key, attributes) {
  const object = {
    id: uniqId(),
    createdAt: null,
    attributes,
  };

  return {
    ...contentType,
    [key]: {
      ...contentType[key],
      byId: {
        ...contentType[key].byId,
        [object.id]: object,
      },
      allIds: [
        ...contentType[key].allIds,
        object.id,
      ],
    },
  };
}

export function updateContentTypeObject(contentType, key, id, attributes) {
  const object = {
    id: id,
    createdAt: null,
    attributes,
  };

  return {
    ...contentType,
    [key]: {
      ...contentType[key],
      byId: {
        ...contentType[key].byId,
        [object.id]: object,
      },
      allIds: [
        ...contentType[key].allIds,
        object.id,
      ],
    },
  };
}

export function removeContentTypeObject(contentType, key, id) {
  const byId = contentType[key].byId;
  delete byId[id];
  return {
    ...contentType,
    [key]: {
      ...contentType[key],
      byId,
      allIds: contentType[key].allIds.filter(itemId => itemId !== id),
    },
  };
}

function addEmptyTab(contentType) {
  return addContentTypeObject(contentType, 'tabs', {
    title: {
      t: 'string',
      v: 'Details',
    },
  });
}

export function createEmptyContentType() {
  const contentType = {
    id: null,
    createdAt: null,
    attributes: {
      title: null,
      description: null,
      slug: null,
    },
    tabs: {
      byId: {},
      allIds: [],
    },
    sections: {
      byId: {},
      allIds: [],
    },
    properties: {
      byId: {},
      allIds: [],
    },
  };

  return addEmptyTab(contentType);
}

export function setDiscountAttr(contentType, label, value) {
  return assoc(contentType,
    ['discounts', 0, 'attributes', label, 'v'], value,
  );
}
