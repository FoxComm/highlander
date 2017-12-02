// @flow

// libs
import pick from 'lodash/pick';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcommerce/wings';
import { createEmptyTaxon, duplicateTaxon } from 'paragons/taxon';

const defaultContext = 'default';

////////////////////////////////////////////////////////////////////////////////
// Internal async actions.
////////////////////////////////////////////////////////////////////////////////

const _fetchTaxon = createAsyncActions(
  'fetchTaxon',
  (taxonId: number, context: string = defaultContext) =>
    Api.get(`/taxons/${context}/${taxonId}`)
);

const _createTaxon = createAsyncActions(
  'createTaxon',
  (taxonomyId: number, taxon: TaxonDraft, context: string = defaultContext) =>
    Api.post(`/taxonomies/${context}/${taxonomyId}/taxons`, pick(taxon, ['attributes', 'location']))
);

const _updateTaxon = createAsyncActions(
  'updateTaxon',
  (taxon: Taxon, context: string = defaultContext) =>
    Api.patch(`/taxons/${context}/${taxon.id}`, pick(taxon, ['attributes', 'location']))
);

const _archiveTaxon = createAsyncActions(
  'archiveTaxon',
  (taxonId: number, context: string = defaultContext) =>
    Api.delete(`/taxons/${context}/${taxonId}`)
);

const _addProduct = createAsyncActions(
  'taxonAddProduct',
  (productId, context: string = defaultContext, taxonId: number) =>
    Api.patch(`/taxons/${context}/${taxonId}/product/${productId}`)
);

const _deleteProduct = createAsyncActions(
  'taxonDeleteProduct',
  (productId, context: string = defaultContext, taxonId: number) =>
    Api.delete(`/taxons/${context}/${taxonId}/product/${productId}`)
);

////////////////////////////////////////////////////////////////////////////////
// Public actions.
////////////////////////////////////////////////////////////////////////////////

export const reset = createAction('TAXON_RESET');
export const duplicate = createAction('TAXON_DUPLICATE');
export const addSubvalue = createAction('TAXON_ADD_SUBVALUE');
export const clearArchiveErrors = _archiveTaxon.clearErrors;

export const create = (taxonomyId: number) => _createTaxon.perform.bind(null, taxonomyId);
export const archive = _archiveTaxon.perform;
export const update = _updateTaxon.perform;
export const addProduct = _addProduct.perform;
export const addProductCurried = (productId: number, context: string) =>
  _addProduct.perform.bind(null, productId, context);
export const deleteProduct = _deleteProduct.perform;
export const deleteProductCurried = (productId: number, context: string) =>
  _deleteProduct.perform.bind(null, productId, context);


export const fetch = (id: string, context: string = defaultContext): ActionDispatch => {
  return dispatch => {
    // WTF??? TODO: get rid of this magic
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

const initialState = createEmptyTaxon();

const reducer = createReducer({
  [reset]: () => initialState,
  [duplicate]: (state) => duplicateTaxon(state),
  [addSubvalue]: (state, parentId) => assoc(initialState, ['location', 'parent'], parentId),
  [_fetchTaxon.succeeded]: (state, taxon) => taxon,
  [_createTaxon.succeeded]: (state, taxon) => taxon,
  [_updateTaxon.succeeded]: (state, taxon) => taxon,
  [_archiveTaxon.succeeded]: () => initialState,
}, initialState);

export default reducer;
