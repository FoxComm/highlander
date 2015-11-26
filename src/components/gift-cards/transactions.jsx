import _ from 'lodash';
import React, { PropTypes } from 'react';
import { createSelector } from 'reselect';
import TableView from '../table/tableview';
import * as GiftCardsTransactionActions from '../../modules/gift-cards/transactions';

import { connect } from 'react-redux';

const mapStateToProps = createSelector(
    state => state.router.params.giftcard,
    state => state.giftCards.transactions,
  (identity, transactions) => ({
    transactions: _.get(transactions, [identity, 'items'])
  })
);

@connect(mapStateToProps, GiftCardsTransactionActions)
export default
class GiftCardTransactions extends React.Component {
  static propTypes = {
    fetchTransactionsIfNeeded: PropTypes.func,
    tableColumns: PropTypes.array,
    params: PropTypes.shape({
      giftcard: PropTypes.string.isRequired
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
    const { giftcard } = this.props.params;

    this.props.fetchTransactionsIfNeeded(this.props.params.giftcard);
  }

  render() {
    return (
      <div>
        <TableView
          columns={this.props.tableColumns}
          data={{
            rows: _.get(this.props.transactions, 'result', []),
            total: _.get(this.props.transactions, ['pagination', 'total'], 0)
          }}
          setState={()=>{}}
          paginator={false}
          />
      </div>
    );
  }
}
