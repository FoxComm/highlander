'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import CreditCardBox from '../credit-cards/card-box';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomersActions from '../../modules/customers/details';

@connect((state, props) => ({
  ...state.customers.details[props.customerId]
}), CustomersActions)
export default class CustomerCreditCards extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchCreditCards: PropTypes.func,
    cards: PropTypes.array
  }

  componentDidMount() {
    const customer = this.props.customerId;

    this.props.fetchCreditCards(customer);
  }

  render() {
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
          {(this.props.cards && this.props.cards.map(createCardBox))}
        </ul>
      </ContentBox>
    );
  }
}
