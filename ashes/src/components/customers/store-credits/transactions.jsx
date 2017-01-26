/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// components
import Summary from './summary';
import SelectableSearchList from '../../list-page/selectable-search-list';
import StoreCreditTransactionRow from './transactions-row';

// redux
import { actions as StoreCreditTransactionsActions } from '../../../modules/customers/store-credit-transactions';
import * as StoreCreditTotalsActions from '../../../modules/customers/store-credit-totals';

type Actions = {
  fetchTotals: Function,
};

type Props = {
  params: Object,
  tableColumns: Array<any>,
  totalsActions: Actions,
  list: Object,
  actions: Object,
};

class StoreCreditTransactions extends Component {
  props: Props;

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
      { term: { accountId: this.customerId } }
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
                 transactionsSelected={true} />
        <div className="fc-store-credits__list">
          <SelectableSearchList
            entity="customers.storeCreditTransactions"
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

export default connect(mapStateToProps, mapDispatchToProps)(StoreCreditTransactions);
