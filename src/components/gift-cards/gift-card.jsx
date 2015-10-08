'use strict';

import React, { PropTypes } from 'react';
import Api from '../../lib/api';
import { IndexLink, Link } from '../link';
import { formatCurrency } from '../../lib/format';
import moment from 'moment';

export default class GiftCard extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      giftcard: PropTypes.string.isRequired
    }).isRequired,
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      card: {}
    };
  }

  componentDidMount() {
    let { giftcard } = this.props.params;

    Api.get(`/gift-cards/${giftcard}`)
      .then((res) => {
        this.setState({
          card: res
        });
      })
      .catch((err) => {
        console.error(err);
      });
  }

  changeState(event) {
    Api.patch(`/gift-cards/${this.state.card.code}`, {state: event.target.value})
      .then((res) => {
        this.setState({
          card: res
        });
      })
      .catch((err) => {
        console.error(err);
      });
  }

  render() {
    let subNav = null;
    let card = this.state.card;
    let status = null;

    const content = React.cloneElement(this.props.children, {'gift-card': card, modelName: 'gift-card' });

    if (card.code) {
      let params = {giftcard: card.code};
      subNav = (
        <div className="gutter">
          <ul className="fc-tabbed-nav">
            <li><IndexLink to="gift-card-transactions" params={params}>Transactions</IndexLink></li>
            <li><Link to="gift-card-notes" params={params}>Notes</Link></li>
            <li><Link to="gift-card-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          {content}
        </div>
      );
    }

    if (card.status === 'Canceled') {
      status = <span>{card.status}</span>;
    } else {
      status = (
        <select value={this.state.card.status} onChange={this.changeState.bind(this)}>
          <option value="active">Active</option>
          <option value="onHold">On Hold</option>
          <option value="canceled">Cancel Gift Card</option>
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
            <div className="fc-col-md-1-3">
              <article className="panel featured available-balance">
                <header>Available Balance</header>
                <p>{ formatCurrency(card.availableBalance) }</p>
              </article>
            </div>
            <div className="fc-col-md-2-3">
              <article className="panel">
                <div className="fc-grid">
                  <div className="fc-col-md-1-2">
                    <p>
                      <strong>Created By: </strong>
                      {card.storeAdmin ? `${card.storeAdmin.firstName} ${card.storeAdmin.lastName}` : 'None'}
                    </p>

                    <p><strong>Recipient: </strong>None</p>

                    <p><strong>Recipient Email: </strong>None</p>

                    <p><strong>Recipient Cell (Optional): </strong>None</p>
                  </div>
                  <div className="fc-col-md-1-2">
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
            <div className="fc-col-md-1-5">
              <article className="panel featured">
                <header>Original Balance</header>
                <p>{ formatCurrency(card.originalBalance) }</p>
              </article>
            </div>
            <div className="fc-col-md-1-5">
              <article className="panel featured">
                <header>Current Balance</header>
                <p>{ formatCurrency(card.currentBalance) }</p>
              </article>
            </div>
            <div className="fc-col-md-1-5">
              <article className="panel featured">
                <header>Date/Time Issued</header>
                <p>{ moment(card.date).format('L LTS') }</p>
              </article>
            </div>
            <div className="fc-col-md-1-5">
              <article className="panel featured">
                <header>Gift Card Type</header>
                <p>{ card.originType }</p>
              </article>
            </div>
            <div className="fc-col-md-1-5">
              <article className="panel featured">
                <header>Current State</header>
                <p>{ status }</p>
              </article>
            </div>
          </div>
        </div>
        {subNav}
      </div>
    );
  }
}
