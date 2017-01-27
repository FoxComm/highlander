// @flow

// libs
import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import { createEmptyTaxonomy } from 'paragons/taxonomy';

// types
import type { Taxonomy } from 'paragons/taxonomy';

const defaultContext = 'default';

////////////////////////////////////////////////////////////////////////////////
// Internal async actions.
////////////////////////////////////////////////////////////////////////////////

const clearTaxonomy = createAction('TAXONOMY_CLEAR');

const _fetchTaxonomy = createAsyncActions(
  'fetchTaxonomy',
  (id: number, context: string = defaultContext) => {
    return Api.get(`/taxonomy/${defaultContext}/${id}`);
  }
);

const _createTaxonomy = createAsyncActions(
  'createTaxonomy',
  (taxonomy: Taxonomy, context: string = defaultContext) => {
    return Api.post(`/taxonomy/${context}`, taxonomy);
  }
);

const _updateTaxonomy = createAsyncActions(
  'updateTaxonomy',
  (taxonomy: Taxonomy, context: string = defaultContext) => {
    return Api.patch(`/taxonomy/${context}/${taxonomy.id}`, taxonomy);
  }
);

const _archiveTaxonomy = createAsyncActions(
  'archiveTaxonomy',
  (id: number, context: string = defaultContext) => {
    return Api.delete(`/taxonomy/${context}/${id}`);
  }
);

////////////////////////////////////////////////////////////////////////////////
// Public actions.
////////////////////////////////////////////////////////////////////////////////

export const taxonomyNew = createAction('TAXONOMY_NEW');
export const clearArchiveErrors = _archiveTaxonomy.clearErrors;

export const createTaxonomy = _createTaxonomy.perform;
export const updateTaxonomy = _updateTaxonomy.perform;
export const archiveTaxonomy = _archiveTaxonomy.perform;

export const fetchTaxonomy = (id: number|string, context: string = defaultContext): ActionDispatch => {
  if (typeof id == 'number') {
    return dispatch(_fetchTaxonomy.perform(id, context));
  } else if (typeof id == 'string' && id.toLowerCase() == 'new') {
    return dispatch(taxonomyNew());
  }

  throw new Error(`Invalid ID ${id} for taxonomy`);
};

////////////////////////////////////////////////////////////////////////////////
// Reducer.
////////////////////////////////////////////////////////////////////////////////

const initialState = { taxonomy: null };

const reducer = createReducer({
  [taxonomyNew]: () => ({ taxonomy: createEmptyTaxonomy(defaultContext, 'hierarchical') }),
  [_fetchTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
  [_createTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
  [_updateTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
  [_archiveTaxonomy.succeeded]: (state, taxonomy) => ({ ...state, taxonomy }),
}, initialState);

export default reducer;
