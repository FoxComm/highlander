'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import Api from '../../lib/api';
import { Link } from 'react-router';
import { formatCurrency } from '../../lib/format';
import moment from 'moment';

export default class GiftCard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      card: {}
    };
  }

  componentDidMount() {
    let
      { router } = this.context,
      cardId     = router.getCurrentParams().giftcard;

    Api.get(`/gift-cards/${cardId}`)
       .then((res) => {
         this.setState({
           card: res
         });
       })
       .catch((err) => { console.log(err); });
  }

  render() {
    let
      subNav = null,
      card = this.state.card;

    if (card.id) {
      let params = {giftcard: card.id};
      subNav = (
        <div className="gutter">
          <ul className="tabbed-nav">
            <li><Link to="gift-card-notes" params={params}>Notes</Link></li>
            <li><Link to="gift-card-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          <RouteHandler giftcard={card} modelName="giftcard"/>
        </div>
      );
    }

    return (
      <div id="gift-card">
        <div className="gutter title">
          <h1>Gift Card { card.cardNumber }</h1>
        </div>
        <div className="gutter grid-row grid-gutter grid-match">
          <div className="grid-unit col-1">
            <article className="panel featured available-balance">
              <header>Available Balance</header>
              <p>{ formatCurrency(card.availableBalance) }</p>
            </article>
          </div>
          <div className="grid-unit col-3">
            <article className="panel">
              <p>Blah</p>
            </article>
          </div>
        </div>
        <div className="gutter grid-row grid-gutter grid-match row-5">
          <div className="grid-unit col-1">
            <article className="panel featured">
              <header>Original Balance</header>
              <p>{ formatCurrency(card.balance) }</p>
            </article>
          </div>
          <div className="grid-unit col-1">
            <article className="panel featured">
              <header>Current Balance</header>
              <p>{ formatCurrency(card.currentBalance) }</p>
            </article>
          </div>
          <div className="grid-unit col-1">
            <article className="panel featured">
              <header>Date/Time Issued</header>
              <p>{ moment(card.date).format('MM/DD/YYYY HH:mm:ss') }</p>
            </article>
          </div>
          <div className="grid-unit col-1">
            <article className="panel featured">
              <header>Gift Card Type</header>
              <p>{ card.type }</p>
            </article>
          </div>
          <div className="grid-unit col-1">
            <article className="panel featured">
              <header>Current State</header>
              <p>{ card.state }</p>
            </article>
          </div>
        </div>
        {subNav}
      </div>
    );
  }
}

GiftCard.contextTypes = {
  router: React.PropTypes.func
};
