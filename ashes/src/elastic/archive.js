import _ from 'lodash';

function archivedFilter(operator) {
  return {
    term: 'archivedAt',
    hidden: true,
    operator,
    value: {
      type: 'exists'
    }
  };
}


export function filterArchived(filters: Array<SearchFilter>) {
  const archiveAtFilter = _.find(filters, {term: 'archivedAt'});
  if (!archiveAtFilter) {
    return [
      archivedFilter('missing'),
      ...filters,
    ];
  } else if (archiveAtFilter.operator == 'neq') {
    return [
      archivedFilter('exists'),
      ...filters
    ];
  }

  return filters;
}
