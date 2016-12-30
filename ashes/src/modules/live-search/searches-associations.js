import Api from 'lib/api';

import { createReducer } from 'redux-act';
import { get, assoc } from 'sprout-data';

import { createNsAction } from './../utils';

const INITIAL_STATE = {
  isFetchingAssociations: false,
  isUpdatingAssociations: false,
  associations: []
};

export default function makeAssociations(namespace) {
  const fetchAssociationsStart = createNsAction(namespace, 'FETCH_ASSOCIATIONS_START');
  const fetchAssociationsSuccess = createNsAction(namespace, 'FETCH_ASSOCIATIONS_SUCCESS');
  const fetchAssociationsFailure = createNsAction(namespace, 'FETCH_ASSOCIATIONS_FAILURE');

  const associateSearchStart = createNsAction(namespace, 'ASSOCIATE_SEARCH_START');
  const associateSearchSuccess = createNsAction(namespace, 'ASSOCIATE_SEARCH_SUCCESS');
  const associateSearchFailure = createNsAction(namespace, 'ASSOCIATE_SEARCH_FAILURE');

  const dissociateSearchStart = createNsAction(namespace, 'DISSOCIATE_SEARCH_START');
  const dissociateSearchSuccess = createNsAction(namespace, 'DISSOCIATE_SEARCH_SUCCESS');
  const dissociateSearchFailure = createNsAction(namespace, 'DISSOCIATE_SEARCH_FAILURE');

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

  const reducer = createReducer({
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
  }, INITIAL_STATE);

  function _fetchAssociationsStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'shares', 'isFetchingAssociations'], true);
  }

  function _fetchAssociationsSuccess(state, payload) {
    return assoc(state,
      ['savedSearches', state.selectedSearch, 'shares', 'associations'], payload,
      ['savedSearches', state.selectedSearch, 'shares', 'isFetchingAssociations'], false
    );
  }

  function _fetchAssociationsFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'shares', 'isFetchingAssociations'], false);
  }

  function _associateSearchStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'shares', 'isUpdatingAssociations'], true);
  }

  function _associateSearchSuccess(state, users) {
    const path = ['savedSearches', state.selectedSearch, 'shares', 'associations'];
    const items = get(state, path, []);

    return assoc(state,
      path, [...items, ...users],
      ['savedSearches', state.selectedSearch, 'shares', 'isUpdatingAssociations'], false
    );
  }

  function _associateSearchFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'shares', 'isUpdatingAssociations'], false);
  }

  function _dissociateSearchStart(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'shares', 'isUpdatingAssociations'], true);
  }

  function _dissociateSearchSuccess(state, userId) {
    const path = ['savedSearches', state.selectedSearch, 'shares', 'associations'];

    const associations = get(state, path, []);
    const newAssociations = associations.filter(({id}) => id !== userId);

    return assoc(state,
      path, newAssociations,
      ['savedSearches', state.selectedSearch, 'shares', 'isUpdatingAssociations'], false
    );
  }

  function _dissociateSearchFailure(state) {
    return assoc(state, ['savedSearches', state.selectedSearch, 'shares', 'isUpdatingAssociations'], false);
  }

  return {
    reducer,
    actions: {
      fetchAssociations,
      associateSearch,
      dissociateSearch,
    }
  };
};
