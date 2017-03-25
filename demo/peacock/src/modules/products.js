/* @flow */

import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { addTaxonomyFilter, addTaxonomiesAggregation, addMustNotFilter, defaultSearch, termFilter, addNestedTermFilter } from 'lib/elastic';
import _ from 'lodash';
import { api } from 'lib/api';

export type Facet = {
  label: string,
  value: Object | string,
  count: number,
}

export type Facets = {
  key: string,
  name: string,
  kind: string,
  values: Array<Facet>,
}

export type Product = {
  id: number;
  context: string,
  title: string;
  description: string,
  images: ?Array<string>,
  currency: string,
  productId: number,
  salePrice: string,
  scope: string,
  skus: Array<string>,
  tags: Array<string>,
  albums: ?Array<Object> | Object,
};

const MAX_RESULTS = 1000;
const context = process.env.FIREBIRD_CONTEXT || 'default';
const GIFT_CARD_TAG = 'GIFT-CARD';

function apiCall(
  categoryNames: ?Array<string>,
  sorting: { direction: number, field: string },
  selectedFacets: Object,
  { ignoreGiftCards = true } = {}): Promise<*> {

  let payload = defaultSearch(context);

  const filteredCats = _.remove(categoryNames, cat => cat != null);
  _.forEach(filteredCats, (cat) => {
    if (cat != 'ALL') {
      payload = addNestedTermFilter(payload, 'taxonomies', 'taxonomies.taxons', cat);
    }
  });

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addMustNotFilter(payload, giftCardTerm);
  }

  const order = sorting.direction === -1 ? 'desc' : 'asc';
    // $FlowFixMe
  payload.sort = [{ [sorting.field]: { order } }];

  payload = addTaxonomiesAggregation(payload);

  _.forEach(selectedFacets, (values, facet) => {
    if(!_.isEmpty(values)) {
      payload = addTaxonomyFilter(payload, facet, values);
    }
  });

  return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload)
  .then((payload) => {
    return {
      payload: payload,
      selectedFacets,
    };
  });
}

function searchGiftCards() {
  return apiCall.call({ api }, [GIFT_CARD_TAG], { direction: 1, field: 'salesPrice' }, { ignoreGiftCards: false });
}

const {fetch, ...actions} = createAsyncActions('products', apiCall);

const initialState = {
  list: [],
  facets: [],
};

function determineFacetKind(f) {
  if(f.includes('color')) return 'color';
  else if(f.includes('size')) return 'circle';
  else return 'checkbox';
}

function titleCase(t) { 
  return _.startCase(_.toLower(t));
}

function mapFacetValue(v, kind) {
  const orig = v;
  if(kind == 'color') {
    let color = _.toLower(v).replace(/\s/g, '');
    v = {color: color, value: orig};
  } 

  return v;
}

function mapAggregationsToFacets(aggregations) { 
    return _.map(aggregations, (a) => {
      const kind = determineFacetKind(a.key);
      const values = _.map(a.taxon.buckets, (t) => {

        return {
          label: titleCase(t.key),
          value: mapFacetValue(t.key, kind),
          count: t.doc_count,
        };
      });
      return {
        key: a.key,
        name: titleCase(a.key),
        kind: kind,
        values: values,
      };
    });
}

const reducer = createReducer({
  [actions.succeeded]: (state, action) => {
    const {payload, selectedFacets} = action;
    const payloadResult = payload.result;
    const payloadAggregations = payload.aggregations.taxonomies.taxonomy.buckets;
    const list = _.isEmpty(payloadResult) ? [] : payloadResult;
    const aggregations = _.isEmpty(payloadAggregations) ? [] : payload.aggregations.taxonomies.taxonomy.buckets;
    const facets = _.isEmpty(selectedFacets) ? mapAggregationsToFacets(aggregations) : state.facets;

    return {
      ...state,
      list: list,
      facets: facets,
    };
  },
}, initialState);

export {
  reducer as default,
  fetch,
  searchGiftCards,
};
