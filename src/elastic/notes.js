import * as dsl from './dsl';
import { addNativeFilters } from './common';

export default function processQuery({ entityType, entityId }, query) {
  return addNativeFilters(query, [
    dsl.termFilter('referenceId', entityId),
    dsl.termFilter('referenceType', entityType),
    dsl.existsFilter('deletedAt', 'missing'),
  ]);
}
