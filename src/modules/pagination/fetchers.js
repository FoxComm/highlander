
import Api from '../../lib/api';

function pickFetchParams(state) {
  return {
    from: state.from,
    size: state.size,
    sortBy: state.sortBy,
  };
}

export function apiStaticUrl(url) {
  return function() {
    return Api.get(url, pickFetchParams(this.searchState));
  };
}

export function apiDynamicUrl(makeUrl) {
  return function(entity) {
    return Api.get(makeUrl(entity), pickFetchParams(this.searchState));
  };
}
