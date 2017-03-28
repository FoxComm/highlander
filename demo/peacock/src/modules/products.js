/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import {
  addTaxonomyFilter,
  addTaxonomiesAggregation,
  addMustNotFilter, defaultSearch, termFilter, addNestedTermFilter, addTermFilter,
} from 'lib/elastic';
import _ from 'lodash';
import { api } from 'lib/api';

import type { Facet } from 'types/facets';

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
  sorting: ?{ direction: number, field: string },
  selectedFacets: Object,
  { ignoreGiftCards = true } = {}): Promise<*> {
  let payload = defaultSearch(context);

  _.forEach(_.compact(categoryNames), (cat) => {
    if (cat !== 'ALL' && cat !== GIFT_CARD_TAG) {
      payload = addNestedTermFilter(payload, 'taxonomies', 'taxonomies.taxons', cat);
    } else if (cat === GIFT_CARD_TAG) {
      const tagTerm = termFilter('tags', cat.toUpperCase());
      payload = addTermFilter(payload, tagTerm);
    }
  });

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addMustNotFilter(payload, giftCardTerm);
  }

  if (sorting) {
    const order = sorting.direction === -1 ? 'desc' : 'asc';
    payload.sort = [{ [sorting.field]: { order } }];
  }
    
  payload = addTaxonomiesAggregation(payload);

  _.forEach(selectedFacets, (values: Array<string>, facet: string) => {
    if (!_.isEmpty(values)) {
      payload = addTaxonomyFilter(payload, facet, values);
    }
  });

  return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload)
  .then((response) => {
    return {
      payload: response,
      selectedFacets,
    };
  });
}

function searchGiftCards() {
  return apiCall.call({ api }, [GIFT_CARD_TAG], null, { ignoreGiftCards: false });
}

const {fetch, ...actions} = createAsyncActions('products', apiCall);

const initialState = {
  list: [],
  facets: [],
};

function determineFacetKind(f: string): string {
  if (f.includes('color')) return 'color';
  else if (f.includes('size')) return 'circle';
  return 'checkbox';
}

function titleCase(t) {
  return _.startCase(_.toLower(t));
}

function mapFacetValue(v, kind) {
  let value = v;
  if (kind == 'color') {
    const color = _.toLower(v).replace(/\s/g, '');
    value = {color, value: v};
  }

  return value;
}

function mapAggregationsToFacets(aggregations): Array<Facet> {
  return _.map(aggregations, (a) => {
    const kind = determineFacetKind(a.key);
    const values = _.uniq(_.map(a.taxon.buckets, (t) => {
      return {
        label: titleCase(t.key),
        value: mapFacetValue(t.key, kind),
        count: t.doc_count,
      };
    }), (v) => {v.label});

    return {
      key: a.key,
      name: titleCase(a.key),
      kind,
      values,
    };
  });
}

const reducer = createReducer({
  [actions.succeeded]: (state, action) => {
    const {payload, selectedFacets} = action;
    const payloadResult = payload.result;
    const aggregations = _.isNil(payload.aggregations) ? [] : payload.aggregations.taxonomies.taxonomy.buckets;
    const list = _.isEmpty(payloadResult) ? [] : payloadResult;

    const queryFacets = mapAggregationsToFacets(aggregations);
    const originalFacets = state.facets;

    let facets = [];

    //the only time this should be empty is on first call.
    if(_.isEmpty(state.facets)) {
      facets = queryFacets;
    } else {
      //merge aggregations from quiries into existing state. 
      //Keep existinged selected facets and only change unselected ones.. 
      //This avoids quiries that would return empty results.
      //While also keeping the interface from changing too much.
      const groupedQueyFacets = _.groupBy(queryFacets, (f) => { return f.key});

      facets = _.map(state.facets, (v) => {
        return (!_.isEmpty(selectedFacets[v.key])) ? v : groupedQueyFacets[v.key][0];
      });
    }

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
