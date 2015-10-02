'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class GiftCardTransactions extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      transactions: []
    };
  }

  componentDidMount() {
    const { giftcard } = this.props.params;

    Api.get(`/gift-cards/${giftcard}/transactions`)
       .then((res) => {
         this.setState({
           transactions: res
         });
       })
       .catch((err) => { console.error(err); });
  }

  render() {
    return (
      <div id="gift-card-transactions">
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={this.state.transactions} model="gift-card-transaction" />
        </table>
      </div>
    );
  }
}

GiftCardTransactions.propTypes = {
  tableColumns: React.PropTypes.array
};

GiftCardTransactions.contextTypes = {
  router: React.PropTypes.func
};

GiftCardTransactions.defaultProps = {
  tableColumns: [
    {field: 'createdAt', text: 'Date/Time', type: 'date'},
    {field: 'orderRef', text: 'Transaction', type: 'link', model: 'order', id: 'orderRef'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'state', text: 'Payment State'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
  ]
};
