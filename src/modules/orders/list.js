import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const searches = [
  {
    name: 'Remorse Hold',
    currentOptions: terms,
    searches: [
      {
        display: 'Order : State : Remorse Hold',
        selectedTerm: 'status',
        selectedOperator: 'eq',
        value: {
          type: 'enum',
          value: 'remorseHold'
        }
      }
    ]
  }, {
    name: 'Manual Hold',
    currentOptions: terms,
    searches: [
      {
        display: 'Order : State : Manual Hold',
        selectedTerm: 'status',
        selectedOperator: 'eq',
        value: {
          type: 'enum',
          value: 'manualHold'
        }
      }
    ]
  }, {
    name: 'Fraud Hold',
    currentOptions: terms,
    searches: [
      {
        display: 'Order : State : Fraud Hold',
        selectedTerm: 'status',
        selectedOperator: 'eq',
        value: {
          type: 'enum',
          value: 'fraudHold'
        }
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch('ORDERS', searchTerms);

export {
  reducer as default,
  actions
};
