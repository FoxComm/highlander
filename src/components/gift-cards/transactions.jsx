
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';

// components
import TableView from '../table/tableview';
import { SearchableList } from '../list-page';
import GiftCardTransactionRow from './gift-card-transaction-row';

// redux
import { actions } from '../../modules/gift-cards/transactions';

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

@connect((state, props) => ({
  list: state.giftCards.transactions,
  giftCard: state.giftCards.details[props.params.giftCard],
}), mapDispatchToProps)
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
      {field: 'orderReferenceNumber', text: 'Transaction', type: 'link', model: 'order', id: 'orderRef'},
      {field: 'debit', text: 'Amount', type: 'transaction'},
      {field: 'state', text: 'Payment State'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
    ]
  };

  get defaultSearchOptions() {
    return {
      singleSearch: true,
      initialFilters: [{
        display: 'Gift Card: ' + this.props.params.giftCard,
        selectedTerm: 'code',
        selectedOperator: 'eq',
        hidden: true,
        value: {
          type: 'string',
          value: this.props.params.giftCard
        }
      }],
    };
  }

  @autobind
  renderRow(row, index, columns, params) {
    const key = `gift-card-${row.code}`;
    return (
      <GiftCardTransactionRow key={key}
                              giftCard={row}
                              columns={columns}
                              params={params} />
    );
  }

  render() {
    return (
      <div className="fc-gift-card-transactions">
        <SearchableList
          emptyResultMessage="No transactions found."
          noGutter={true}
          list={this.props.list}
          renderRow={this.renderRow}
          tableColumns={this.props.tableColumns}
          searchActions={this.props.actions}
          searchOptions={this.defaultSearchOptions} />
      </div>
    );
  }
}
