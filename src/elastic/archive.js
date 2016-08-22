import _ from 'lodash';

import type { SearchFilter } from 'elastic/common';

export function filterArchived(filters: Array<SearchFilter>) {
  if (!_.find(filters, {term: 'archivedAt'})) {
    filters = [
      {
        term: 'archivedAt',
        hidden: true,
        operator: 'missing',
        value: {
          type: 'exists'
        }
      },
      ...filters,
    ];
  }
  
  return filters;
}