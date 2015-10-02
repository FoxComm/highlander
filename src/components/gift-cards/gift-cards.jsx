'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import NewGiftCard from './gift-cards-new';
import GiftCardStore from '../stores/gift-cards';

export default class GiftCards extends React.Component {
  constructor(props) {
    super(props);
    this.state = _.assign({}, GiftCardStore.getState(), {isNew: false});
  }

  componentDidMount() {
    GiftCardStore.listenTo(this.onChange);

    GiftCardStore.fetchGiftCards();
  }

  componentWillUnmount() {
    GiftCardStore.unlisten(this.onChange);
  }

  onChange(state) {
    this.setState(state);
  };

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
      for (let row of rows) {
        row.classList.remove('new');
      }
    }, 5000);
  }

  render() {
    let content = null;

    if (this.state.isNew) {
      content = <NewGiftCard />;
    } else {
      content = (
        <div id="cards">
          <div className="gutter">
            <h2>Gift Cards</h2>
            <button onClick={this.toggleNew.bind(this)}>+ New Gift Card</button>
            <TableView
              columns={this.props.tableColumns}
              rows={this.state.cards}
              model='giftcard'
              />
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
    {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
    {field: 'originType', text: 'Type'},
    {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'date', text: 'Date Issued', type: 'date'}
  ]
};
