'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import CreditCardBox from '../credit-cards/card-box';
import NewCreditCardBox from '../credit-cards/new-card-box';
import { AddButton } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomerCreditCardActions from '../../modules/customers/credit-cards';

@connect((state, props) => ({
  ...state.customers.creditCards[props.customerId]
}), CustomerCreditCardActions)
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

  @autobind
  onAddClick() {
    console.log("onAddClick");
    const customer = this.props.customerId;

    this.props.newCustomerCreditCard(customer);
  }

  @autobind
  onAddingCancel() {
    console.log('onAddingCancel');
    const customer = this.props.customerId;

    this.props.cancelNewCustomerCreditCard(customer);
  }

  render() {
    let actionBlock = (
      <AddButton onClick={this.onAddClick} />
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
          {(this.props.newCreditCard && <NewCreditCardBox onCancel={ this.onAddingCancel }/>)}
        </ul>
      </ContentBox>
    );
  }
}
