
export function query(query, rest = {}) {
  return {query, ...rest};
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

export function matchQuery(query, field = '_all', {type = 'phrase_prefix', max_expansions = 10, ...rest} = {}) {
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

export function nestedTermFilter(term, value) {
  const path = term.slice(0, term.lastIndexOf('.'));

  return nestedQuery(path, termFilter(term, value));
}

export function sortByField(field, order = 'asc') {
  return {
    [field]: {order}
  };
}
