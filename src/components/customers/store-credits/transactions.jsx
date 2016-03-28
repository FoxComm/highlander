
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
import SelectableSearchList from '../../list-page/selectable-search-list';
import StoreCreditTransactionRow from './transactions-row';

// redux
import { actions as StoreCreditTransactionsActions } from '../../../modules/customers/store-credit-transactions';
import * as StoreCreditTotalsActions from '../../../modules/customers/store-credit-totals';

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
    totalsActions: PropTypes.shape({
      fetchTotals: PropTypes.func,
    }).isRequired,
    list: PropTypes.object,
    actions: PropTypes.object,
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
        text: 'Transaction',
      },
      {
        field: 'debit',
        text: 'Amount',
        type: 'transaction'
      },
      {
        field: 'state',
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

  componentDidMount() {
    this.props.actions.setExtraFilters([
      {term: {customerId: this.customerId}}
    ]);
    this.props.totalsActions.fetchTotals(this.customerId);
    this.props.actions.fetch();
  }

  renderRow(row, index, columns, params) {
    const key = `sc-transaction-${row.id}`;
    return (
      <StoreCreditTransactionRow key={key}
                                 storeCreditTransaction={row}
                                 columns={columns}
                                 params={params} />
    );
  }

  render() {
    const totals = _.get(this.props, ['storeCreditTotals', 'totals'], {});

    return (
      <div className="fc-store-credits">
        <Summary totals={totals}
                 params={this.props.params}
                 history={this.context.history}
                 transactionsSelected={true} />
        <div className="fc-list-page-content fc-store-credits__list">
          <SelectableSearchList
            title="Transactions"
            emptyMessage="No transactions found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={this.props.tableColumns}
            searchActions={this.props.actions}
            searchOptions={{singleSearch: true}} />
        </div>
      </div>
    );
  }
}
