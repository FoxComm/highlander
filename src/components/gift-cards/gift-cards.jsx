'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as giftCardActions from '../../modules/gift-cards';
import _ from 'lodash';

@connect(state => _.pick(state, 'giftCards'), giftCardActions)
export default class GiftCards extends React.Component {

  componentDidMount() {
    this.props.fetchGiftCardsIfNeeded();
  }

  onCardsAdded(cards) {
    let cardList = this.state.cards.slice(0, this.state.cards.length);

    this.toggleNew();

    for (let card of cards) {
      card.isNew = true;
    }

    Array.prototype.unshift.apply(cardList, cards);
    this.setState({
      cards: cardList
    });

    this.removeNew();
  }

  removeNew() {
    setTimeout(() => {
      let rows = [].slice.call(document.querySelectorAll('tr.new'));
      for (let row of rows) {
        row.classList.remove('new');
      }
    }, 5000);
  }

  render() {
    const items = this.props.giftCards.items || [];

    return (
      <div id="cards">
        <div className="gutter">
          <h2>Gift Cards</h2>
          <Link to='gift-cards-new' className="fc-btn">+ New Gift Card</Link>
          <TableView
              columns={this.props.tableColumns}
              rows={items}
              model='giftcard'
          />
        </div>
      </div>
    );
  }
}

GiftCards.propTypes = {
  tableColumns: React.PropTypes.array
};

GiftCards.defaultProps = {
  tableColumns: [
    {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
    {field: 'originType', text: 'Type'},
    {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'date', text: 'Date Issued', type: 'date'}
  ]
};
