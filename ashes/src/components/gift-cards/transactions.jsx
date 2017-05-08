
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';

// components
import { SelectableSearchList } from '../list-page';
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
      {field: 'orderPayment', text: 'Order', type: 'id', model: 'order', id: 'orderRef'},
      {field: 'debit', text: 'Amount', type: 'transaction'},
      {field: 'state', text: 'Payment State'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      {term: {code: this.props.params.giftCard}}
    ]);
    this.props.actions.fetch();
  }

  @autobind
  renderRow(row, index, columns, params) {
    const key = `gift-card-${index}`;
    return (
      <GiftCardTransactionRow
        key={key}
        giftCard={row}
        columns={columns}
        params={params}
      />
    );
  }

  render() {
    return (
      <div className="fc-gift-card-transactions">
        <SelectableSearchList
          entity="giftCards.transactions"
          emptyMessage="No transactions found."
          list={this.props.list}
          renderRow={this.renderRow}
          tableColumns={this.props.tableColumns}
          searchActions={this.props.actions}
          searchOptions={{singleSearch: true}} />
      </div>
    );
  }
}
