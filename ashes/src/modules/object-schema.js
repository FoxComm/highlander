
import { createAsyncActions } from '@foxcomm/wings';
import { createReducer, createAction } from 'redux-act';
import Api from 'lib/api';

const _fetchSchema = createAsyncActions(
  'fetchSchema',
  (kind, id = void 0) => {
    if (kind === 'sku') {
      const skuSchema = require('./schemas/sku.json');
      return new Promise(resolve => resolve([{schema: skuSchema}]));
    }

    return Api.get(`/object/schemas/byKind/${kind}`);
  }
);
export const saveSchema = createAction('SCHEMA_SAVE', (kind, schema) => [kind, schema]);

export const fetchSchema = _fetchSchema.perform;

const initialState = {};

const reducer = createReducer({
  [_fetchSchema.succeeded]: (state, results) => {
    const result = results[0];

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
