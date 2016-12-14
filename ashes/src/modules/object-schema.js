
import _ from 'lodash';
import createAsyncActions from './async-utils';
import { createReducer, createAction } from 'redux-act';
import Api from 'lib/api';

const _fetchSchema = createAsyncActions(
  'fetchSchema',
  (kind, id = void 0) => Api.get(`/object/schemas/byKind/${kind}`)
);
export const saveSchema = createAction('SCHEMA_SAVE', (kind, schema) => [kind, schema]);

export const fetchSchema = _fetchSchema.perform;

const initialState = {};

// TODO: Remove me please
const skuSchema = require('../../../phoenix-scala/resources/object_schemas/sku.json');

const reducer = createReducer({
  [_fetchSchema.succeeded]: (state, results) => {
    const result = results[0];

    // TODO: and me
    if (result.kind == "sku") {
      return {
        ...state,
        [result.kind]: skuSchema,
      };
    }

    return {
      ...state,
      [result.kind]: result.schema,
    };
  },
  [saveSchema]: (state, [kind, schema]) => {
    return {
      ...state,
      [kind]: schema,
    };
  }
}, initialState);


export default reducer;
