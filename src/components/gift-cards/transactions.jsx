'use strict';

import React, { PropTypes } from 'react';
import Api from '../../lib/api';
import { createSelector } from 'reselect';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import * as GiftCardsTransactionActions from '../../modules/gift-cards/transactions';

import { connect } from 'react-redux';

const mapStateToProps = createSelector(
  state => state.router.params.giftcard,
  state => state.giftCards.transactions,
  (identity, transactions) => ({
    transactions: transactions[identity] && transactions[identity].items || []
  })
);

@connect(mapStateToProps, GiftCardsTransactionActions)
export default class GiftCardTransactions extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    params: PropTypes.shape({
      giftcard: PropTypes.string.isRequired
    }).isRequired
  };

  static defaultProps = {
    tableColumns: [
      {field: 'createdAt', text: 'Date/Time', type: 'date'},
      {field: 'orderRef', text: 'Transaction', type: 'link', model: 'order', id: 'orderRef'},
      {field: 'amount', text: 'Amount', type: 'currency'},
      {field: 'state', text: 'Payment State'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
    ]
  };

  componentDidMount() {
    const { giftcard } = this.props.params;

    this.props.fetchTransactionsIfNeeded(this.props.params.giftcard);
  }

  render() {
    return (
      <div id="gift-card-transactions">
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={this.props.transactions} model="gift-card-transaction" />
        </table>
      </div>
    );
  }
}
