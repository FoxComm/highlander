export function query(query, rest = {}) {
  return { query, ...rest };
}

export function existsFilter(field, operator) {
  return {
    [operator]: { field }
  };
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

export function matchQuery(field, query) {
  return {
    match: {
      [field]: query
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

export function ids(values) {
  return {
    ids: {
      values
    }
  };
}

export function nestedTermFilter(term, value) {
  const path = term.slice(0, term.lastIndexOf('.'));

  return nestedQuery(path, termFilter(term, value));
}

export function nestedMatchFilter(term, value) {
  const path = term.slice(0, term.lastIndexOf('.'));

  return nestedQuery(path, matchQuery(term, value));
}

export function sortByField(field, order = { order: 'asc' }) {
  return {
    [field]: order
  };
}
