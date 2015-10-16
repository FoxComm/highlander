'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableView from '../tables/tableview';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as giftCardActions from '../../modules/gift-cards';

@connect(state => ({items: state.giftCards.items}), giftCardActions)
export default class GiftCards extends React.Component {

  static propTypes = {
    tableColumns: PropTypes.array,
    items: PropTypes.array
  };

  static defaultProps = {
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
    return (
      <div id="cards">
        <div className="gutter">
          <h2>Gift Cards</h2>
          <Link to='gift-cards-new' className="fc-btn">+ New Gift Card</Link>
          <TableView
              columns={this.props.tableColumns}
              rows={this.props.items}
              model='giftcard'
          />
        </div>
      </div>
    );
  }
}
