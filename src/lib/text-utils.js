import { singularize, pluralize } from 'fleck';

export function numberize(entity, count) {
  return count === 1 ? singularize(entity) : pluralize(entity);
}
