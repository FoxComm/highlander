//libs
import _ from 'lodash';
import { createReducer } from 'redux-act';

//data
import criterions from '../../../paragons/customer-groups/criterions';
import operators from '../../../paragons/customer-groups/operators';
import queryAdapter from '../query-adapter';
import makePagination, { makeFetchAction } from '../../pagination/base';
import { addPaginationParams } from '../../pagination';
import { ConditionAnd, ConditionOr, Field } from '../query';

//helpers
import * as search from '../../../lib/search';


const getStateBranch = state => _.get(state, 'customerGroups.dynamic');

let { reducer, ...actions } = makePagination('customer-groups-dynamic', null, null, {});

function fetcher() {
  const { searchState, getState } = this;

  const {mainCondition, conditions, filterTerm} = getStateBranch(getState()).group;

  const query = queryAdapter(criterions, mainCondition, conditions);

  if (filterTerm) {
    let condition;
    if (query.main instanceof ConditionAnd) {
      condition = query.main;
    } else {
      condition = query.and();
      condition.add(query.main);
      query.main = condition;
    }

    condition.add(
      query.or().set([
        query.field('name').add(operators.contains, filterTerm),
        query.field('email').add(operators.contains, filterTerm)
      ])
    );
  }

  const request = query.toRequest();

  console.debug('searching', request, searchState);
  return search.post(addPaginationParams('customers_search_view/_search', searchState), request);
}

actions.fetch = makeFetchAction(fetcher, actions, state => getStateBranch(state).list);

// for overriding updateStateAndFetch in pagination actions
actions.updateStateAndFetch = (newState, ...args) => {
  return dispatch => {
    dispatch(actions.updateState(newState));
    dispatch(actions.fetch(...args));
  };
};

export {
  reducer as default,
  actions
};
