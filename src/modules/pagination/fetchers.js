
import Api from '../../lib/api';

function pickFetchParams(state) {
  return {
    from: state.from,
    size: state.size,
    sortBy: state.sortBy,
  };
}

export function apiStaticUrl(url) {
  return ({state}) => {
    return Api.get(url, pickFetchParams(state));
  };
}

export function apiDynamicUrl(makeUrl) {
  return (entity, {state}) => {
    return Api.get(makeUrl(entity), pickFetchParams(state));
  };
}
