function processItem(item) {
  let parts = item.node_path.split('/');
  const text = parts.pop();
  const prefix = parts.join(' » ');
  const id = item.node_id;

  return {id, prefix, text};
}

/**
 * Converts Hyperion suggest response to <Suggest /> component format
 */
export function getSuggest(response) {
  let ret = {};
  let primary = null;
  let secondary = null;

  if (response.primary && response.primary.length) {
    ret.primary = response.primary.map(processItem);
  }

  if (response.secondary && response.secondary.length) {
    ret.secondary = response.secondary.map(processItem);
  }

  return ret;
}
