'use strict';

import React, { PropTypes } from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import GiftCardTransactionsStore from '../../stores/gift-card-transactions';
import GiftCardTransactionActions from '../../actions/gift-card-transactions';

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


  constructor(props, context) {
    super(props, context);
    this.state = {
      data: GiftCardTransactionsStore.getState()
    };
    this.onChange = this.onChange.bind(this);
  }

  componentDidMount() {
    const { giftcard } = this.props.params;

    GiftCardTransactionsStore.listen(this.onChange);

    GiftCardTransactionActions.fetchTransactions(giftcard);
  }

  componentWillUnmount() {
    GiftCardTransactionsStore.unlisten(this.onChange);
  }

  onChange() {
    this.setState({
      data: GiftCardTransactionsStore.getState()
    });
  }

  render() {
    return (
      <div id="gift-card-transactions">
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={this.state.data.get('transactions').toArray()} model="gift-card-transaction" />
        </table>
      </div>
    );
  }
}
