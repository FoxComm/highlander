// @flow

// libs
import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import { createEmptyTaxon, duplicateTaxon } from 'paragons/taxon';
import _ from 'lodash';

const defaultContext = 'default';

////////////////////////////////////////////////////////////////////////////////
// Internal async actions.
////////////////////////////////////////////////////////////////////////////////

const clearTaxon = createAction('TAXON_CLEAR');

const _fetchTaxon = createAsyncActions(
  'fetchTaxon',
  (taxonId: number, context: string = defaultContext) =>
    Api.get(`/taxons/${context}/${taxonId}`)
);

const _createTaxon = createAsyncActions(
  'createTaxon',
  (taxonomyId: number, taxon: TaxonDraft, context: string = defaultContext) =>
    Api.post(`/taxonomies/${context}/${taxonomyId}/taxons`, taxon)
);

const _updateTaxon = createAsyncActions(
  'updateTaxon',
  (taxon: Taxon, context: string = defaultContext) =>
    Api.patch(`/taxons/${context}/${taxon.id}`, taxon)
);

const _archiveTaxon = createAsyncActions(
  'archiveTaxon',
  (taxonId: number, context: string = defaultContext) =>
    Api.delete(`/taxons/${context}/${taxonId}`)
);

////////////////////////////////////////////////////////////////////////////////
// Public actions.
////////////////////////////////////////////////////////////////////////////////

export const reset = createAction('TAXON_RESET');
export const duplicate = createAction('TAXON_DUPLICATE');
export const clearArchiveErrors = _archiveTaxon.clearErrors;

export const create = (taxonomyId: string) => _createTaxon.perform.bind(null, taxonomyId);
export const archive = _archiveTaxon.perform;
export const update = _updateTaxon.perform;

export const fetch = (id: string, context: string = defaultContext): ActionDispatch => {
  return dispatch => {
    if (id.toString().toLowerCase() === 'new') {
      return dispatch(reset());
    } else {
      return dispatch(_fetchTaxon.perform(id, context));
    }
  };
};

////////////////////////////////////////////////////////////////////////////////
// Reducer.
////////////////////////////////////////////////////////////////////////////////

const initialState = { taxon: createEmptyTaxon() };

const reducer = createReducer({
  [reset]: () => initialState,
  [duplicate]: (state) => ({
    ...initialState,
    taxon: duplicateTaxon(_.get(state, 'taxon', {}))
  }),
  [_fetchTaxon.succeeded]: (state, taxon) => ({ ...state, taxon }),
  [_createTaxon.succeeded]: (state, taxon) => ({ ...state, taxon }),
  [_updateTaxon.succeeded]: (state, taxon) => ({ ...state, taxon }),
  [_archiveTaxon.succeeded]: (state, taxon) => ({ ...state, taxon })
}, initialState);

export default reducer;
