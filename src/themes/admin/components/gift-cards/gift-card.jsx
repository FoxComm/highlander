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
      cardCode     = router.getCurrentParams().giftcard;

    Api.get(`/gift-cards/${cardCode}`)
       .then((res) => {
         this.setState({
           card: res
         });
       })
       .catch((err) => { console.error(err); });
  }

  changeState(event) {
    Api.patch(`/gift-cards/${this.state.card.code}`, {state: event.target.value})
       .then((res) => {
         this.setState({
           card: res
         });
       })
       .catch((err) => { console.error(err); });
  }

  render() {
    let
      subNav = null,
      card   = this.state.card,
      state  = null;

    if (card.code) {
      let params = {giftcard: card.code};
      subNav = (
        <div className="gutter">
          <ul className="fc-tabbed-nav">
            <li><Link to="gift-card-transactions" params={params}>Transactions</Link></li>
            <li><Link to="gift-card-notes" params={params}>Notes</Link></li>
            <li><Link to="gift-card-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          <RouteHandler gift-card={card} modelName="gift-card"/>
        </div>
      );
    }

    if (card.state === 'Canceled') {
      state = <span>{card.status}</span>;
    } else {
      state = (
        <select defaultValue={card.status} onChange={this.changeState.bind(this)}>
          <option value="Active">Active</option>
          <option value="On Hold">On Hold</option>
          <option value="Canceled">Cancel Gift Card</option>
        </select>
      );
    }

    return (
      <div id="gift-card">
        <div className="gutter title">
          <h1>Gift Card { card.code }</h1>
        </div>
        <div className="gutter">
          <div className="fc-grid fc-grid-match fc-grid-gutter">
            <div className="fc-col-1-3">
              <article className="panel featured available-balance">
                <header>Available Balance</header>
                <p>{ formatCurrency(card.availableBalance) }</p>
              </article>
            </div>
            <div className="fc-col-2-3">
              <article className="panel">
                <div className="fc-grid">
                  <div className="fc-col-1-2">
                    <p>
                      <strong>Customer:</strong>
                      {card.customer ? `${card.customer.firstName} ${card.customer.lastName}` : 'None'}
                    </p>
                    <p><strong>Recipient:</strong> None</p>
                    <p><strong>Recipient Email:</strong> None</p>
                    <p><strong>Recipient Cell (Optional):</strong> None</p>
                  </div>
                  <div className="fc-col-1-2">
                    <p><strong>Message (optional):</strong></p>
                    <p>
                      {card.message}
                    </p>
                  </div>
                </div>
              </article>
            </div>
          </div>
          <div className="fc-grid fc-grid-match fc-grid-gutter">
            <div className="fc-col-1-5">
              <article className="panel featured">
                <header>Original Balance</header>
                <p>{ formatCurrency(card.originalBalance) }</p>
              </article>
            </div>
            <div className="fc-col-1-5">
              <article className="panel featured">
                <header>Current Balance</header>
                <p>{ formatCurrency(card.currentBalance) }</p>
              </article>
            </div>
            <div className="fc-col-1-5">
              <article className="panel featured">
                <header>Date/Time Issued</header>
                <p>{ moment(card.date).format('L LTS') }</p>
              </article>
            </div>
            <div className="fc-col-1-5">
              <article className="panel featured">
                <header>Gift Card Type</header>
                <p>{ card.type }</p>
              </article>
            </div>
            <div className="fc-col-1-5">
              <article className="panel featured">
                <header>Current State</header>
                <p>{ state }</p>
              </article>
            </div>
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
