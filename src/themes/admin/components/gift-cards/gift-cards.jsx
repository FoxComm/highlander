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
      for (let i = 0; i < rows.length; i++) {
        rows[i].classList.remove('new');
      }
    }, 5000);
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
              <TableBody columns={this.props.tableColumns} rows={this.state.cards} model="giftcard" />
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
    {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'id'},
    {field: 'type', text: 'Type'},
    {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'date', text: 'Date Issued', type: 'date'}
  ]
};
