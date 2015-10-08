'use strict';

import React, { PropTypes } from 'react';
import CreditCardDetails from './card-details';
import ItemCardContainer from '../item-card-container/item-card-container';

export default class CreditCardBox extends React.Component {

  static propTypes = {
    card: PropTypes.object,
    customerId: PropTypes.number.isRequired,
  };

  constructor(props, context) {
    super(props, context);
  }

  render() {
    let card = this.props.card;

    let isDefault = (
      <label className="fc-address-default">
        <input type="checkbox" defaultChecked={card.isDefault} />
        <span>Default card</span>
      </label>
    );

    let buttons = (
      <div>
        <button className="fc-btn icon-trash"></button>
      </div>
    );

    return (
      <ItemCardContainer className="fc-credit-cards"
                         leftControls={ isDefault }
                         rightControls={ buttons }>
        <CreditCardDetails customerId={ this.props.customerId} card={ this.props.card } />
      </ItemCardContainer>
    );
  }
}
