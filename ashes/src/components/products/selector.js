/**
 * @flow
 */

export type SuggestItem = {
  id: string,
  path: string,
  prefix: string,
  text: string,
};

function processItem(item): SuggestItem {
  let parts = item.node_path.split('/');
  const text = parts.pop();
  const prefix = parts.join(' » ');
  const id = item.node_id;
  const path = item.node_path;

  return { id, path, prefix, text };
}

/**
 * Converts Hyperion suggest response to <Suggest /> component format
 */
export function getSuggest(response: any): Array<SuggestItem> {
  // @todo find a way to search by title in hyperion

  return (response.secondary || []).map(processItem);
}
