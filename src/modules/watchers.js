// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// data
import { searchAdmins } from '../elastic/store-admins';
import { entityForms } from '../paragons/watcher';

// helpers
import { getSingularForm, getPluralForm } from '../lib/text-utils';


/**
 * What is stored:
 * {
 *  [group]: {
 *    listModalDisplayed: Boolean (whether modal window with full list of group watchers is displayed)
 *  },
 *  selectModal: {
 *    group: String (one of valid watcher groups for currently open add modal),
 *    displayed: Boolean (whether add modal is displayed),
 *    suggested: Object[] (array of watchers' entries suggested in modal),
 *    selected: Number[] (array of watchers' ids that are selected for add/remove action)
 *  }
 * }
 */
export const creators = {
  toggleListModal: (state, group) => {
    const path = [group, 'listModalDisplayed'];
    const oldValue = _.get(state, path, false);

    return assoc(state, path, !oldValue);
  },
  showSelectModal: (state, group) => {
    return assoc(state,
      ['selectModal', 'group'], group,
      ['selectModal', 'displayed'], true
    );
  },
  hideSelectModal: (state) => {
    return assoc(state,
      ['selectModal', 'group'], null,
      ['selectModal', 'displayed'], false
    );
  },
  selectItem: (state, item) => {
    const path = ['selectModal', 'selected'];

    const items = _.get(state, path, []);

    if (_.findIndex(items, ({id}) => id === item.id) < 0) {
      return assoc(state, path, items.concat(item));
    }

    return state;
  },
  deselectItem: (state, index) => {
    const path = ['selectModal', 'selected'];

    const items = _.get(state, path, []);
    const newItems = _.without(items, items[index]);

    return assoc(state, path, newItems);
  },
  clearSelected: (state) => {
    const path = ['selectModal', 'selected'];

    return assoc(state, path, []);
  },
  setSuggested: (state, payload) => {
    return assoc(state, ['selectModal', 'suggested'], payload);
  },
  failWatchersAction: (state, error) => {
    console.error(error);

    return state;
  },
};

export const actions = {
  suggestWatchers: (term, actions) => {
    return dispatch => {
      searchAdmins(term).then(
        ({result}) => dispatch(actions.setSuggested(_.isEmpty(result) ? [] : result)),
        () => dispatch(actions.setSuggested([]))
      );
    };
  }
};

export const initialState = {
  selectModal: {
    group: null,
    displayed: false,
    suggested: [],
    selected: [],
  },
};
