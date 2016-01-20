import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const searches = [
  {
    name: 'Remorse Hold',
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

const { reducer, actions } = makeLiveSearch('orders', searchTerms, searches);

export {
  reducer as default,
  actions
};
