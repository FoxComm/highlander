import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, get, dissoc, merge, update } from 'sprout-data';
import { criteriaOptions, criteriaOperators } from './constants';
import { fetchRegions } from '../regions';
import { groupCount, groupCriteriaToRequest } from '../../elastic/customers';

//<editor-fold desc="funcs">
const _createAction = (description, ...args) => {
  return createAction('GROUP_BUILDER_' + description, ...args);
};

function append(collection, item) {
  return assoc(collection, collection.length, item);
}

function newVal(state, id, term, type) {
  const val = {type};

  switch(term) {
    case 'region':
      val['options'] = state.groups.builder.staticData.regions;
      break;
  }
  switch(type) {
    case 'bool':
      val['value'] = true;
      break;
    case 'enum':
      if (_.isEmpty(val['options'])) {
        val['options'] = get(criteriaOptions, [term, 'options']);
      }
      break;
  }
  return val;
}

function newCrit(state, id, term) {
  const nullCrit = {
    selectedTerm: null, operators: {}, selectedOperator: null, value: {}
  };
  if (!term) {
    return nullCrit;
  }
  const type = get(criteriaOptions, [term, 'type']);
  if (!type) {
    console.error(`Can't find type for term ${newTerm}`);
    return nullCrit;
  }
  const operators = criteriaOperators[type];
  if (!operators) {
    console.error(`Can't find operators for type`, type);
    return nullCrit;
  }
  return {
    selectedTerm: term,
    operators: operators,
    selectedOperator: _.first(_.keys(operators)),
    value: newVal(state, id, term, type)
  };
}

const currentTerms = state => {
  return _.pluck(_.values(state.criterions), 'selectedTerm');
};
//</editor-fold>

//<editor-fold desc="data">
const terms = _.keys(criteriaOptions);

const initialState = {
  counter: 2,
  termOptions: _.reduce(criteriaOptions, (r, v, k) => assoc(r, k, v.title), {}),
  matchCriteria: 'all',
  criterions: {1: newCrit()},
  staticData: {}
};
//</editor-fold>

//<editor-fold desc="actions">
const prepareData = _createAction('PREPARE_DATA');
// -
// ES actions
const searchStarted = _createAction('ES_STARTED');
const searchCompleted = _createAction('ES_COMPLETED');
const searchFailed = _createAction('ES_FAILED');

// -- fn that create actions will triger ES query for count customers
const _createStateChangeAction = (description, ...args) => {
  const f = _createAction(description, ...args);

  function action(...actionArgs) {
    return (dispatch, getState) => {
      dispatch(f(...actionArgs));

      dispatch(searchStarted());
      const state = getState().groups.builder;
      const criteria = state.criterions;
      if (!_.isEmpty(criteria)) {
        groupCount(criteria, state.matchCriteria).then(
          results => dispatch(searchCompleted(results)) && results,
          errors => dispatch(searchFailed(errors)) && errors);
      }
    };
  }

  action.toString = () => 'GROUP_BUILDER_' + description;

  return action;
};
// - criteria change actions
const addCriterionAction = _createStateChangeAction('ADD_CRITERIA');
export const removeCriterion = _createStateChangeAction('REMOVE_CRITERIA');
const updateCriteria = _createStateChangeAction('UPDATE_CRITERIA', (id, newCrit) => [id, newCrit]);
export const changeOperator = _createStateChangeAction('CHANGE_OPERATOR', (id, newOpVal) => [id, newOpVal]);
export const changeValue = _createStateChangeAction('CHANGE_VALUE', (id, newVal) => [id, newVal]);
export const changeMatchCriteria = _createStateChangeAction('CHANGE_MATCHING');
// - api actions
const groupSaved = _createAction('SAVED');
const groupSaveFailed = _createAction('SAVE_FAILED');

function dumpState(state) {
  return {
    criteria: state.criterions,
    matchCriteria: state.matchCriteria,
  };
}

export function saveQuery(name) {
  return (dispatch, getState) => {
    const state = getState().groups.builder;

    const data = {
      name: name,
      clientState: dumpState(state),
      customersCount: state.searchResultsLength,
      elasticRequest: groupCriteriaToRequest(state.criterions, state.matchCriteria),
    };
    Api.post(`/groups`, data)
      .then(
        group => dispatch(groupSaved(group)),
        err => dispatch(groupSaveFailed(err))
      );
  };
}

// TODO: use ES7 await here when we update babel to 6.x
export function initBuilder() {
  const updateState = (dispatch, getState) => {
    const regions = getState().regions;
    dispatch(prepareData({
      regions: _.reduce(regions, (r, v, k) => assoc(r, k, v.name), {})
    }));
  };

  return (dispatch, getState) => {
    if (_.contains(criteriaOptions, 'region')) {
      if (_.isEmpty(getState().regions)) {
        dispatch(fetchRegions()).then(() => updateState(dispatch, getState));
      } else {
        updateState(dispatch, getState);
      }
    }
  };
}

export function changeTerm(id, newTerm) {
  return (dispatch, getState) => {
    const updatedCrit = newCrit(getState(), id, newTerm);
    dispatch(updateCriteria(id, updatedCrit));
  };
}

export function addCriterion() {
  return (dispatch, getState) => {
    const state = getState().groups.builder;
    dispatch(addCriterionAction());

    const newSize = _.size(state.criterions);
    // pre set term if we have only last one
    const lastAdded = newSize == terms.length && newSize - _.size(state.criterions) == 1;
    if (lastAdded) {
      const term = _.first(_.difference(terms, currentTerms(state)));
      dispatch(changeTerm(state.counter, term));
    }
  };
}
//</editor-fold>

const reducer = createReducer({
  [prepareData]: (state, data) => {
    return assoc(state, 'staticData', data);
  },
  [changeMatchCriteria]: (state, value) => {
    return assoc(state, 'matchCriteria', value);
  },
  [searchStarted]: (state) => {
    return assoc(state,
      'esStart', true,
      'searchErrors', null
    );
  },
  [searchCompleted]: (state, results) => {
    return assoc(state,
      'esStart', false,
      'searchResultsLength', get(results, ['count']),
      'searchResults', results
    );
  },
  [searchFailed]: (state, errors) => {
    return assoc(state,
      'esStart', false,
      'searchErrors', errors
    );
  },
  [addCriterionAction]: state => {
    if (_.contains(currentTerms(state), null)) {
      // don't allow to add two empty criteria
      return state;
    }

    if (terms.length == _.size(state.criterions)) {
      // nothing to add
      return state;
    }

    return assoc(state,
      'counter', state.counter + 1,
      'criterions', assoc(state.criterions, state.counter, newCrit())
    );
  },
  [removeCriterion]: (state, id) => {
    if (_.size(state.criterions) === 1) {
      // empty criteria if it's last one.
      return assoc(state, ['criterions', id], newCrit());
    }
    return assoc(state, 'criterions', dissoc(state.criterions, id));
  },
  [updateCriteria]: (state, [id, newCrit]) => {
    return assoc(state, ['criterions', id], merge(state.criterions[id], newCrit));
  },
  [changeOperator]: (state, [id, newOpVal]) => {
    return assoc(state, ['criterions', id, 'selectedOperator'], newOpVal);
  },
  [changeValue]: (state, [id, newVal]) => {
    return assoc(state, ['criterions', id, 'value', 'value'], newVal);
  }
}, initialState);

export default reducer;
