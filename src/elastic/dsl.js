
export function query(query) {
  return {query};
}

export function termFilter(field, value) {
  return {
    term: {
      [field]: value
    }
  };
}

export function rangeFilter(field, operations) {
  return {
    range: {
      [field]: operations
    }
  };
}

export function matchQuery(query, {field = '_all', type = 'phrase_prefix', max_expansions = 10, ...rest} = {}) {
  return {
    match: {
      [field]: {
        query,
        type,
        max_expansions,
        ...rest
      }
    }
  };
}

export function nestedQuery(path, query) {
  return {
    nested: {
      path,
      query
    }
  };
}

export function sortByField(field, order = 'asc') {
  return {
    [field]: {order}
  };
}
