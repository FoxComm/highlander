import _ from 'lodash';
import React, { PropTypes } from 'react';
import { createSelector } from 'reselect';
import TableView from '../table/tableview';
import * as GiftCardsTransactionActions from '../../modules/gift-cards/transactions';

import { connect } from 'react-redux';

@connect(state => state.giftCards, GiftCardsTransactionActions)
export default class GiftCardTransactions extends React.Component {
  static propTypes = {
    fetch: PropTypes.func,
    setFetchParams: PropTypes.func,
    actionReset: PropTypes.func,
    setGiftCard: PropTypes.func,
    tableColumns: PropTypes.array,
    params: PropTypes.shape({
      giftCard: PropTypes.string.isRequired
    }).isRequired,
    transactions: PropTypes.any
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
    const { giftCard } = this.props.params;

    this.props.actionReset(); // clean state from previous values
    this.props.setGiftCard(giftCard); // set gift card id for further requests
    this.props.fetch({giftCard});
  }

  render() {
    return (
      <div>
        <TableView
          columns={this.props.tableColumns}
          data={this.props.transactions}
          setState={this.props.setFetchParams}
          paginator={true}
          />
      </div>
    );
  }
}
