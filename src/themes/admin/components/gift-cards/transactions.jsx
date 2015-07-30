'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class GiftCardTransactions extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      transactions: []
    };
  }

  componentDidMount() {
    let
      { router } = this.context,
      cardId     = router.getCurrentParams().giftcard;
    Api.get(`/gift-cards/${cardId}/transactions`)
       .then((res) => {
         this.setState({
           transactions: res
         });
       })
       .catch((err) => { console.log(err); });
  }

  render() {
    return (
      <div id="gift-card-transactions">
        <table className="inline">
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
    {field: 'orderRef', text: 'Transaction', type: 'link', model: 'order', id: 'orderId'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'state', text: 'Payment State'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
  ]
};
