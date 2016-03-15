import _ from 'lodash';

import Api from '../../lib/api';

import { createReducer } from 'redux-act';
import { get, assoc } from 'sprout-data';

import { createNsAction } from './../utils';
import { searchAdmins } from '../../elastic/store-admins';

const INITIAL_STATE = {
  isFetchingAssociations: false,
  isUpdatingAssociationsAssociations: false,
  isFetchingSuggestions: false,
  associations: []
};

export default function makeAssociations(namespace) {

  /** Search associations management. Internal actions */
  const suggestAssociationsStart = createNsAction(namespace, 'SUGGEST_ASSOCIATIONS_START');
  const suggestAssociationsSuccess = createNsAction(namespace, 'SUGGEST_ASSOCIATIONS_SUCCESS');
  const suggestAssociationsFailure = createNsAction(namespace, 'SUGGEST_ASSOCIATIONS_FAILURE');

  const fetchAssociationsStart = createNsAction(namespace, 'FETCH_ASSOCIATIONS_START');
  const fetchAssociationsSuccess = createNsAction(namespace, 'FETCH_ASSOCIATIONS_SUCCESS');
  const fetchAssociationsFailure = createNsAction(namespace, 'FETCH_ASSOCIATIONS_FAILURE');

  const associateSearchStart = createNsAction(namespace, 'ASSOCIATE_SEARCH_START');
  const associateSearchSuccess = createNsAction(namespace, 'ASSOCIATE_SEARCH_SUCCESS');
  const associateSearchFailure = createNsAction(namespace, 'ASSOCIATE_SEARCH_FAILURE');

  const dissociateSearchStart = createNsAction(namespace, 'DISSOCIATE_SEARCH_START');
  const dissociateSearchSuccess = createNsAction(namespace, 'DISSOCIATE_SEARCH_SUCCESS');
  const dissociateSearchFailure = createNsAction(namespace, 'DISSOCIATE_SEARCH_FAILURE');


  /** Exported actions */
  const suggestAssociations = term => {
    return dispatch => {
      dispatch(suggestAssociationsStart());

      searchAdmins(term).then(
        res => dispatch(suggestAssociationsSuccess(res.result)),
        err => dispatch(suggestAssociationsFailure())
      );
    };
  };

  const fetchAssociations = (search) => {
    return dispatch => {
      dispatch(fetchAssociationsStart());

      return Api.get(`/shared-search/${search.code}/associates`)
        .then(
          searches => dispatch(fetchAssociationsSuccess(searches)),
          err => dispatch(fetchAssociationsFailure(err))
        );
    };
  };

  const associateSearch = (search, users) => {
    const ids = users.map(user => user.id);

    return dispatch => {
      dispatch(associateSearchStart());

      return Api.post(`/shared-search/${search.code}/associate`, { associates: ids })
        .then(
          res => {
            dispatch(clearSelected());
            dispatch(associateSearchSuccess(users));
          },
          err => dispatch(associateSearchFailure())
        );
    };
  };

  const dissociateSearch = (search, userId) => {
    return dispatch => {
      dispatch(dissociateSearchStart());

      return Api.delete(`/shared-search/${search.code}/associate/${userId}`)
        .then(
          res => dispatch(dissociateSearchSuccess(userId)),
          err => dispatch(dissociateSearchFailure())
        );
    };
  };

  /** Typeahead actions */
  const selectItem = createNsAction(namespace, 'TYPEAHEAD_SELECT_ITEM');
  const deselectItem = createNsAction(namespace, 'TYPEAHEAD_DESELECT_ITEM');
  const clearSelected = createNsAction(namespace, 'TYPEAHEAD_CLEAR_SELECTED');
  const setTerm = createNsAction(namespace, 'TYPEAHEAD_SET_TERM');


  const reducer = createReducer({
    /** search list of admins by term */
    [suggestAssociationsStart]: (state) => _suggestAssociationsStart(state),
    [suggestAssociationsSuccess]: (state, payload) => _suggestAssociationsSuccess(state, payload),
    [suggestAssociationsFailure]: (state, err) => _suggestAssociationsFailure(state, err),

    /** fetch search associations with other admins */
    [fetchAssociationsStart]: (state) => _fetchAssociationsStart(state),
    [fetchAssociationsSuccess]: (state, payload) => _fetchAssociationsSuccess(state, payload),
    [fetchAssociationsFailure]: (state, err) => _fetchAssociationsFailure(state, err),

    /** associating searches to other admins */
    [associateSearchStart]: (state, payload) => _associateSearchStart(state, payload),
    [associateSearchSuccess]: (state, payload) => _associateSearchSuccess(state, payload),
    [associateSearchFailure]: (state, err) => _associateSearchFailure(state, err),

    /** dissociating searches from other admins */
    [dissociateSearchStart]: (state) => _dissociateSearchStart(state),
    [dissociateSearchSuccess]: (state, payload) => _dissociateSearchSuccess(state, payload),
    [dissociateSearchFailure]: (state, err) => _dissociateSearchFailure(state, err),

    /** handle typeahead state for admins search */
    [selectItem]: (state, payload) => _selectItem(state, payload),
    [deselectItem]: (state, payload) => _deselectItem(state, payload),
    [clearSelected]: (state, payload) => _clearSelected(state, payload),
    [setTerm]: (state, err) => _setTerm(state, err),
  }, INITIAL_STATE);

  /** Reducers functions */
  function _suggestAssociationsStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isFetchingSuggestions'], true);
  }

  function _suggestAssociationsSuccess(state, list) {
    const associations = get(state, ['savedSearches', state.selectedSearch, 'associations'], []);

    /** skip already associated users */
    list = list.filter(suggestion => _.findIndex(associations, ({ id }) => id === suggestion.id) < 0);

    return assoc(state,
      ['savedSearches', state.selectedSearch, 'suggested'], list,
      ['savedSearches', state.selectedSearch, 'isFetchingSuggestions'], false
    );
  }

  function _suggestAssociationsFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isFetchingSuggestions'], false);
  }

  function _fetchAssociationsStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isFetchingAssociations'], true);
  }

  function _fetchAssociationsSuccess(state, payload) {
    return assoc(state,
      ['savedSearches', state.selectedSearch, 'associations'], payload,
      ['savedSearches', state.selectedSearch, 'isFetchingAssociations'], false
    );
  }

  function _fetchAssociationsFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isFetchingAssociations'], false);
  }

  function _associateSearchStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isUpdatingAssociations'], true);
  }

  function _associateSearchSuccess(state, users) {
    const path = ['savedSearches', state.selectedSearch, 'associations'];
    const items = get(state, path, []);

    return assoc(state,
      path, [...items, ...users],
      ['savedSearches', state.selectedSearch, 'isUpdatingAssociations'], false
    );
  }

  function _associateSearchFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isUpdatingAssociations'], false);
  }

  function _dissociateSearchStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isUpdatingAssociations'], true);
  }

  function _dissociateSearchSuccess(state, userId) {
    const path = ['savedSearches', state.selectedSearch, 'associations'];

    const associations = get(state, path, []);
    const newAssociations = associations.filter(({id}) => id !== userId);

    return assoc(state,
      path, newAssociations,
      ['savedSearches', state.selectedSearch, 'isUpdatingAssociations'], false
    );
  }

  function _dissociateSearchFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'isUpdatingAssociations'], false);
  }


  function _selectItem(state, item) {
    const path = ['savedSearches', state.selectedSearch, 'selected'];

    const items = get(state, path, []);

    if (_.findIndex(items, ({ id }) => id === item.id) < 0) {
      return assoc(state, path, [...items, item]);
    }

    return state;
  }

  function _deselectItem(state, index) {
    const path = ['savedSearches', state.selectedSearch, 'selected'];

    const items = get(state, path, []);
    const newItems = _.without(items, items[index]);

    return assoc(state, path, newItems);
  }

  function _clearSelected(state) {
    const path = ['savedSearches', state.selectedSearch, 'selected'];

    return assoc(state, path, []);
  }

  function _setTerm(state, term) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'term'], term);
  }

  return {
    reducer,
    actions: {
      suggestAssociations,
      fetchAssociations,
      associateSearch,
      dissociateSearch,
      selectItem,
      deselectItem,
      setTerm,
    }
  };
};
