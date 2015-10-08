'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import CreditCardBox from '../credit-cards/card-box';

import CreditCardsStore from '../../stores/credit-cards';

export default class CustomerCreditCards extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      cards: []
    };
  }

  componentDidMount() {
    CreditCardsStore.listenToEvent('change', this);
    CreditCardsStore.fetch(this.props.customerId);
  }

  componentWillUnmount() {
    CreditCardsStore.stopListeningToEvent('change', this);
  }

  onChangeCreditCardsStore(customerId, cards) {
    if (customerId != this.props.customerId) return;
    this.setState({
      cards: cards
    });
  }

  render() {
    console.log(this.state);
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );

    let createCardBox = (card) => {
      let key = `cutomer-card-${ card.id }`;
      return (
        <CreditCardBox key={ key }
                       card={ card }
                       customerId={ this.props.customerId } />
      );
    };

    return (
      <ContentBox title="Credit Cards"
                  className="fc-customer-credit-cards"
                  actionBlock={ actionBlock }>
        <ul className="fc-float-list">
          {this.state.cards.map(createCardBox)}
        </ul>
      </ContentBox>
    );
  }
}
