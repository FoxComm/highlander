
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';

// components
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import MultiSelectTable from '../../table/multi-select-table';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';
import { Checkbox } from '../../checkbox/checkbox';
import State from '../../common/state';
import SearchableList from '../../list-page/searchable-list';

// redux
import { actions as StoreCreditTransactionsActions } from '../../../modules/customers/store-credit-transactions';
import * as StoreCreditTotalsActions from '../../../modules/customers/store-credit-totals';

// const actions = {
//   searchActions: StoreCreditTransactionsActions,
//   ...StoreCreditTotalsActions
// };

const mapStateToProps = (state, props) => ({
  list: state.customers.storeCreditTransactions,
  storeCreditTotals: state.customers.storeCreditTotals[props.params.customerId]
});

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(StoreCreditTransactionsActions, dispatch),
    totalsActions: bindActionCreators(StoreCreditTotalsActions, dispatch)
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class StoreCreditTransactions extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  static propTypes = {
    params: PropTypes.object,
    tableColumns: PropTypes.array,
    fetchStoreCreditTransactions: PropTypes.func,
    fetchTotals: PropTypes.func
  };

  static defaultProps = {
    tableColumns: [
      {
        field: 'createdAt',
        text: 'Date/Time',
        type: 'date'
      },
      {
        field: 'transaction',
        text: 'Transaction'
      },
      {
        field: 'debit',
        text: 'Amount',
        type: 'transaction'
      },
      {
        field: 'status',
        text: 'Payment State',
        type: 'state',
        model: 'storeCreditTransaction'
      },
      {
        field: 'availableBalance',
        text: 'Total Available Balance',
        type: 'currency'
      }
    ]
  };

  get customerId() {
    return this.props.params.customerId;
  }

  get searchUrl() {
    return 'store_credit_transactions_view/_search';
  }

  componentDidMount() {
    this.props.totalsActions.fetchTotals(this.customerId);
  }

  get defaultSearchOptions() {
    return {
      singleSearch: true,
      initialFilters: [{
        display: "Customer: " + this.customerId,
        selectedTerm: "customer.customerId",
        selectedOperator: "eq",
        hidden: true,
        value: {
          type: "string",
          value: '' + this.customerId
        }
      }],
    };
  }

  renderRow(row) {
    return (
      <TableRow key={`storeCreditTransaction-row-${row.id}`}>
        <TableCell><Checkbox /></TableCell>
        <TableCell><DateTime value={row.createdAt}/></TableCell>
        <TableCell>{row.transaction}</TableCell>
        <TableCell><Currency value={-row.debit} isTransaction={true}/></TableCell>
        <TableCell><State value={row.status} model={"storeCreditTransaction"}/></TableCell>
        <TableCell><Currency value={row.availableBalance} /></TableCell>
      </TableRow>
    );
  }

  @autobind
  setState(params) {
    if (params.sortBy) {
      const sort = {};
      const newState = {sortBy: params.sortBy};

      let sortOrder = this.state.sortOrder;

      if (params.sortBy == this.state.sortBy) {
        sortOrder = newState['sortOrder'] = sortOrder == 'asc' ? 'desc' : 'asc';
      }

      sort[params.sortBy] = {order: sortOrder};
      props.searchActions.fetch(props.url, {sort: [sort]});
      this.setState(newState);
    }
  }

  render() {
    const props = this.props;
    const totals = _.get(props, ['storeCreditTotals', 'totals'], {});
    console.log(this.props);

    const selectedSearch = props.storeCreditTransactions.selectedSearch;
    const results = props.storeCreditTransactions.savedSearches[selectedSearch].results;

    const filter = searchTerms => props.searchActions.addSearchFilter(this.searchUrl, searchTerms);
    const selectSearch = idx => props.searchActions.selectSearch(this.searchUrl, idx);

    return (
      <div className="fc-store-credits">
        <Summary totals={totals}
                 params={props.params}
                 history={this.context.history}
                 transactionsSelected={true} />
        <div className="fc-list-page-content">
          {/*<SearchableList
            tableColumns={props.tableColumns}
            toggleColumnPresent={false}
            renderRow={this.renderRow}
            emptyMessage="No transactions found."
            url={this.searchUrl}
            selectSavedSearch={selectSearch}
            submitFilters={filter}
            searches={props.storeCreditTransactions}
            searchActions = {props.searchActions}
            {...this.defaultSearchOptions} >
          </SearchableList>*/}
          <SearchableList
            emptyResultMessage="No transactions found."
            list={props.list}
            renderRow={this.renderRow}
            tableColumns={props.tableColumns}
            searchActions={props.actions}
            url={this.searchUrl} />
        </div>
      </div>
    );
  }
}
