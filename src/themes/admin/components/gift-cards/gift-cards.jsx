'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import NewGiftCard from './gift-cards-new';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const
  createEvent = 'cards-added';

export default class GiftCards extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      cards: [],
      isNew: false
    };
  }

  componentDidMount() {
    listenTo(createEvent, this);
    Api.get('/gift-cards')
       .then((cards) => { this.setState({cards: cards}); })
       .catch((err) => { console.log(err); });
  }

  componentWillUnmount() {
    stopListeningTo(createEvent, this);
  }

  toggleNew() {
    this.setState({
      isNew: !this.state.isNew
    });
  }

  onCardsAdded(cards) {
    let cardList = this.state.cards;

    this.toggleNew();

    Array.prototype.unshift.apply(cardList, cards);
    this.setState({
      cards: cardList
    });
  }

  render() {
    let content = null;

    if (this.state.isNew) {
      content = <NewGiftCard />;
    } else {
      content = (
        <div id="cards" className="gutter">
          <h2>Gift Cards</h2>
          <button onClick={this.toggleNew.bind(this)}>+ New Gift Card</button>
          <div className="gutter">
            <table className="inline">
              <TableHead columns={this.props.tableColumns} />
              <TableBody columns={this.props.tableColumns} rows={this.state.cards} model="gift-card" />
            </table>
          </div>
        </div>
      );
    }
    return content;
  }
}

GiftCards.propTypes = {
  tableColumns: React.PropTypes.array
};

GiftCards.defaultProps = {
  tableColumns: [
    {field: 'cardNumber', text: 'Gift Card Number'},
    {field: 'type', text: 'Type'},
    {field: 'balance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'state', text: 'State'},
    {field: 'date', text: 'Date Issued', type: 'date'}
  ]
};
