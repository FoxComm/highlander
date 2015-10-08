'use strict';

import React, { PropTypes } from 'react';
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
        <span>Default shipping address</span>
      </label>
    );

    let buttons = (
      <div>
        <button className="fc-btn icon-trash"></button>
        <button className="fc-btn icon-edit"></button>
      </div>
    );

    return (
      <ItemCardContainer className="fc-credit-cards"
                         leftControls={ isDefault }
                         rightControls={ buttons }>
        {card.id}
      </ItemCardContainer>
    );
  }
}
