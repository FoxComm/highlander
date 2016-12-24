// libs
import _ from 'lodash';
import { get, assoc } from 'sprout-data';

// data
import { searchAdmins } from '../elastic/store-admins';

// helpers
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

const fetchWatchers = entityType => (actions, entityId, group) => dispatch => {
  dispatch(actions.fetchStart(entityId, group));

  Api.get(`/${entityType}/${entityId}/${group}`).then(
    response => dispatch(actions.fetchSuccess(entityId, group, response)),
    error => dispatch(actions.fetchFailed(entityId, group, error))
  );
};

const _fetchWatchers = createAsyncActions(
  'fetchWatchers',
  function(entityType, entityId, group) {
    return Api.get(`/${entityType}/${entityId}/${group}`)
  }
);

const addWatchers = entityType => (actions, entityId) => (dispatch, getState) => {
  const state = getState();
  const path = [entityType, 'watchers', entityId, 'selectModal'];

  const group = _.get(state, [...path, 'group']);
  const items = _.get(state, [...path, 'selected'], []);

  const data = {
    assignees: items.map((item) => item.id)
  };

  Api.post(`/${entityType}/${entityId}/${group}`, data).then(
    () => {
      dispatch(actions.clearSelected(entityId));
      dispatch(actions.hideSelectModal(entityId));
      dispatch(actions.fetchWatchers(entityId, group));
    },
    (error) => dispatch(actions.failWatchersAction(error))
  );
};

const watch = entityType => (actions, entityId, group, id) => dispatch => {
  const data = {
    assignees: [id]
  };

  Api.post(`/${entityType}/${entityId}/${group}`, data).then(
    () => dispatch(actions.fetchWatchers(entityId, group)),
    (error) => dispatch(actions.failWatchersAction(error))
  );
};

const removeWatcher = entityType => (actions, entityId, group, id) => dispatch => {
  Api.delete(`/${entityType}/${entityId}/${group}/${id}`).then(
    () => dispatch(actions.fetchWatchers(entityId, group)),
    (error) => dispatch(actions.failWatchersAction(error))
  );
};

const suggestWatchers = entityType => (actions, entityId) => (dispatch, getState) => {
  const state = getState();
  const term = _.get(state, [entityType, 'watchers', entityId, 'selectModal', 'term']);

  dispatch(actions.suggestWatchersStart(entityId));
  searchAdmins(term).then(
    ({ result }) => dispatch(actions.setSuggested(entityId, _.isEmpty(result) ? [] : result)),
    () => dispatch(actions.setSuggested(entityId, []))
  );
};

/**
 * What is stored:
 * {
 *  [group]: {
 *    listModalDisplayed: Boolean (whether modal window with full list of group watchers is displayed)
 *  },
 *  selectModal: {
 *    group: String (one of valid watcher groups for currently open add modal),
 *    displayed: Boolean (whether add modal is displayed),
 *    term: String (entered search term)
 *    suggested: Object[] (array of watchers' entries suggested in modal),
 *    selected: Number[] (array of watchers' ids that are selected for add/remove action)
 *  }
 * }
 */
const reducers = {
  fetchStart: (state, [entityId, group]) => {
    return assoc(state, [entityId, group, 'isFetching'], true);
  },
  fetchSuccess: (state, [entityId, group, result]) => {
    return assoc(state,
      [entityId, group, 'entries'], result.map(item => item.assignee),
      [entityId, group, 'isFetching'], false
    );
  },
  fetchFailed: (state, [entityId, group, error]) => {
    return assoc(state, [entityId, group, 'isFetching'], false);
  },
  suggestWatchersStart: (state, entityId) => {
    return assoc(state, [entityId, 'selectModal', 'isFetching'], true);
  },
  toggleListModal: (state, [entityId, group]) => {
    const path = [entityId, group, 'listModalDisplayed'];
    const oldValue = _.get(state, path, false);

    return assoc(state, path, !oldValue);
  },
  showSelectModal: (state, [entityId,group]) => {
    const path = [entityId, 'selectModal'];

    return assoc(state,
      [...path, 'group'], group,
      [...path, 'displayed'], true
    );
  },
  hideSelectModal: (state, entityId) => {
    const path = [entityId, 'selectModal'];

    return assoc(state,
      [...path, 'group'], null,
      [...path, 'displayed'], false
    );
  },
  selectItem: (state, [entityId, item]) => {
    const path = [entityId, 'selectModal', 'selected'];

    const items = _.get(state, path, []);

    if (_.findIndex(items, ({ id }) => id === item.id) < 0) {
      return assoc(state, path, [...items, item]);
    }

    return state;
  },
  deselectItem: (state, [entityId, index]) => {
    const path = [entityId, 'selectModal', 'selected'];

    const items = _.get(state, path, []);
    const newItems = _.without(items, items[index]);

    return assoc(state, path, newItems);
  },
  clearSelected: (state, entityId) => {
    const path = [entityId, 'selectModal', 'selected'];

    return assoc(state, path, []);
  },
  setTerm: (state, [entityId, term]) => {
    return assoc(state, [entityId, 'selectModal', 'term'], term);
  },
  setSuggested: (state, [entityId, payload]) => {
    const path = [entityId, 'selectModal'];

    return assoc(state,
      [...path, 'isFetching'], false,
      [...path, 'suggested'], payload
    );
  },
  failWatchersAction: (state, [entityId, error]) => {
    console.error(error);

    return state;
  },
};

export default entityType => createStore({
  path: `${entityType}.watchers`,
  actions: {
    watch: watch(entityType),
    suggestWatchers: suggestWatchers(entityType),
    fetchWatchers: fetchWatchers(entityType),
    addWatchers: addWatchers(entityType),
    removeWatcher: removeWatcher(entityType),
  },
  reducers,
});
