
import { assoc } from 'sprout-data';

function addEmptyTab(contentType) {
  const tab = {
    id: null,
    createdAt: null,
    attributes: {
      title: {
        t: 'string',
        v: 'Details',
      },
    }
  };

  contentType.tabs.push(tab);
  return contentType;
}

export function createEmptyContentType() {
  const contentType = {
    id: null,
    createdAt: null,
    attributes: {
      title: null,
      description: null,
      slug: null
    },
    tabs: [],
    sections: [],
    properties: [],
    'property-settings': []
  };

  return addEmptyTab(contentType);
}

export function setDiscountAttr(contentType, label, value) {
  return assoc(contentType,
    ['discounts', 0, 'attributes', label, 'v'], value,
  );
}
