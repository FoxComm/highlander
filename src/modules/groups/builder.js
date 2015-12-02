import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, get, dissoc, merge, update } from 'sprout-data';
import { criteriaOptions, criteriaOperators } from './constants';
import { fetchRegions } from '../regions';

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
    case 'revenue':
      val['value'] = '1000000';
      break;
    case 'region':
      val['options'] = state.groups.builder.staticData.regions;
      break;
  }
  switch(type) {
    case 'bool':
      val['value'] = 't';
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
  criterions: {1: newCrit()},
  staticData: {}
};
//</editor-fold>

//<editor-fold desc="actions">
const addCriterionAction = _createAction('ADD_CRITERIA');
export const removeCriterion = _createAction('REMOVE_CRITERIA');
const updateCriteria = _createAction('UPDATE_CRITERIA', (id, newCrit) => [id, newCrit]);
const prepareData = _createAction('PREPARE_DATA');
export const changeOperator = _createAction('CHANGE_OPERATOR', (id, newOpVal) => [id, newOpVal]);
export const changeValue = _createAction('CHANGE_VALUE', (id, newVal) => [id, newVal]);

// TODO: use ES7 await here when we update babel to 6.x
export function initBuilder() {
  const updateState = (dispatch, getState) => {
    const regions = getState().regions;
    dispatch(prepareData({
      regions: _.reduce(regions, (r, v, k) => assoc(r, k, v.name), {})
    }));
  };

  return (dispatch, getState) => {
    if (_.isEmpty(getState().regions)) {
      dispatch(fetchRegions()).then(() => updateState(dispatch, getState));
    } else {
      updateState(dispatch, getState);
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

    const newSize = _.size(getState().groups.builder.criterions);
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
