// @flow

// libs
import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import { createEmptyTaxonomy, duplicateTaxonomy } from 'paragons/taxonomy';
import _ from 'lodash';

const defaultContext = 'default';

////////////////////////////////////////////////////////////////////////////////
// Internal async actions.
////////////////////////////////////////////////////////////////////////////////

const _fetchTaxonomy = createAsyncActions(
  'fetchTaxonomy',
  (id: number, context: string = defaultContext) => {
    return Api.get(`/taxonomies/${context}/${id}`);
  }
);

const _createTaxonomy = createAsyncActions(
  'createTaxonomy',
  (taxonomy: TaxonomyDraft, context: string = defaultContext) => {
    return Api.post(`/taxonomies/${context}`, taxonomy);
  }
);

const _updateTaxonomy = createAsyncActions(
  'updateTaxonomy',
  (taxonomy: Taxonomy, context: string = defaultContext) => {
    return Api.patch(`/taxonomies/${context}/${taxonomy.id}`, taxonomy);
  }
);

const _archiveTaxonomy = createAsyncActions(
  'archiveTaxonomy',
  (id: number, context: string = defaultContext) => {
    return Api.delete(`/taxonomies/${context}/${id}`);
  }
);

////////////////////////////////////////////////////////////////////////////////
// Public actions.
////////////////////////////////////////////////////////////////////////////////

export const reset = createAction('TAXONOMY_RESET');
export const duplicate = createAction('TAXONOMY_DUPLICATE');
export const clearFetchErrors = _fetchTaxonomy.clearErrors;
export const clearArchiveErrors = _archiveTaxonomy.clearErrors;

export const create = _createTaxonomy.perform;
export const update = _updateTaxonomy.perform;
export const archive = _archiveTaxonomy.perform;
export const fetchTaxonomyInternal = _fetchTaxonomy;

export const fetch = (id: string, context: string = defaultContext): ActionDispatch => {
  return dispatch => {
    if (id.toString().toLowerCase() === 'new') {
      return dispatch(reset());
    } else {
      return dispatch(_fetchTaxonomy.perform(id, context));
    }
  };
};

////////////////////////////////////////////////////////////////////////////////
// Reducer.
////////////////////////////////////////////////////////////////////////////////

const initialState = () => ({ taxonomy: createEmptyTaxonomy(defaultContext, false) });

const reducer = createReducer({
  [reset]: () => initialState(),
  [duplicate]: (state) => ({
    ...initialState(),
    taxonomy: duplicateTaxonomy(_.get(state, 'taxonomy', {}))
  }),
  [_fetchTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
  [_createTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
  [_updateTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
  [_archiveTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
}, initialState());

export default reducer;
