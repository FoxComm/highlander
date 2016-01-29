
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import TableView from '../table/tableview';
import { SearchableList } from '../list-page';
import GiftCardTransactionRow from './gift-card-transaction-row';

// redux
import { actions as GiftCardsTransactionActions } from '../../modules/gift-cards/transactions';

@connect(state => state.giftCards, GiftCardsTransactionActions)
export default class GiftCardTransactions extends React.Component {
  static propTypes = {
    fetch: PropTypes.func,
    initialFetch: PropTypes.func,
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
      {field: 'amount', text: 'Amount', type: 'transaction'},
      {field: 'state', text: 'Payment State'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
    ]
  };

  get giftCard() {
    return this.props.params.giftCard;
  }

  get defaultSearchOptions() {
    return {
      singleSearch: true,
    };
  }

  @autobind
  renderRow(row, index, columns) {
    const key = `gift-card-${row.code}`;
    return <GiftCardTransactionRow giftCard={row} columns={columns} key={key}/>;
  }

  render() {
    return (
      <SearchableList
        emptyResultMessage="No transactions found."
        list={this.props.list}
        renderRow={this.renderRow}
        tableColumns={this.props.tableColumns}
        searchActions={this.props}
        searchOptions={this.defaultSearchOptions} />
    );
  }
}
