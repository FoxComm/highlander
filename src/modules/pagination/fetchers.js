
import Api from '../../lib/api';

function pickFetchParams(state) {
  return {
    from: state.from,
    size: state.size,
    sortBy: state.sortBy,
  };
}

export function apiStaticUrl(url) {
  return ({searchState}) => {
    return Api.get(url, pickFetchParams(searchState));
  };
}

export function apiDynamicUrl(makeUrl) {
  return (entity, {searchState}) => {
    return Api.get(makeUrl(entity), pickFetchParams(searchState));
  };
}
