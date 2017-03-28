function processItem(item) {
  let parts = item.node_path.split('/');
  const text = parts.pop();
  const prefix = parts.join(' » ');
  const id = item.node_id;
  const path = item.node_path;

  return {id, path, prefix, text};
}

/**
 * Converts Hyperion suggest response to <Suggest /> component format
 */
export function getSuggest(response) {
  const ret = {};

  if (response.primary && response.primary.length) {
    ret.primary = response.primary.map(processItem);
  }

  if (response.secondary && response.secondary.length) {
    ret.secondary = response.secondary.map(processItem);
  }

  return ret;
}
