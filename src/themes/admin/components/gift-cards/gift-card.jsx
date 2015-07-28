'use strict';

import React from 'react';
import { Link } from 'react-router';

export default class GiftCard extends React.Component {
  render() {
    let
      subNav = null,
      giftCard = this.props.giftCard,
      params = {giftcard: giftCard.id};

    subNav = (
      <div className="gutter">
        <ul className="tabbed-nav">
          <li><Link to="order-notes" params={params}>Notes</Link></li>
        </ul>
        <RouteHandler giftcard={giftCard} modelName="giftcard"/>
      </div>
    );
    return (
      <div id="gift-card">
        {subNav}
      </div>
    );
  }
}

GiftCard.propTypes = {
  giftCard: React.PropTypes.object
};
