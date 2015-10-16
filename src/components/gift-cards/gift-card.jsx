'use strict';

import React, { PropTypes } from 'react';
import { IndexLink, Link } from '../link';
import { formatCurrency } from '../../lib/format';
import moment from 'moment';
import GiftCardStore from '../../stores/gift-cards';
import GiftCardActions from '../../actions/gift-cards';
import SectionTitle from '../section-title/section-title';

export default class GiftCard extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      giftcard: PropTypes.string.isRequired
    }).isRequired,
    children: PropTypes.node
  };

  get giftCard() {
    let { giftcard } = this.props.params;
    return this.state.data.find(item => item.code === giftcard);
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      data: GiftCardStore.getState()
    };
    this.onChange = this.onChange.bind(this);
  }

  componentDidMount() {
    let { giftcard } = this.props.params;
    GiftCardStore.listen(this.onChange);

    GiftCardActions.fetchGiftCard(giftcard);
  }

  componentWillUnmount() {
    GiftCardStore.unlisten(this.onChange);
  }

  onChange() {
    this.setState({
      data: GiftCardStore.getState()
    });
  }

  changeState(event) {
    let card = this.giftCard;

    GiftCardActions.editGiftCard(card.code, {status: event.target.value});
  }

  get subNav() {
    const params = {giftcard: this.giftCard.code};

    if (!this.giftCard.code) {
      return null;
    }

    const content = React.cloneElement(this.props.children, {'gift-card': this.giftCard, modelName: 'gift-card' });

    return (
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

  get status() {
    const card = this.giftCard;

    if (card.status === 'Canceled') {
      return <span>{card.status}</span>;
    } else {
      return (
        <select value={card.status} onChange={this.changeState.bind(this)}>
          <option value="active">Active</option>
          <option value="onHold">On Hold</option>
          <option value="canceled">Cancel Gift Card</option>
        </select>
      );
    }
  }

  resendGiftCard() {
    console.log('Resend');
  }

  render() {
    let card = this.giftCard;

    if (!card) {
      return <div id="gift-card"></div>;
    }

    return (
      <div>
        <SectionTitle title="Gift Card" subtitle={this.giftCard.code}>
          <button onClick={this.resendGiftCard.bind(this)} className="fc-btn fc-btn-primary">Resend Gift Card</button>
        </SectionTitle>
        <div className="fc-grid">
          <div className="fc-col-md-1-3">
            <article className="fc-panel">
              <header className="fc-panel-header">Available Balance</header>
              <p>{ formatCurrency(card.availableBalance) }</p>
            </article>
          </div>
        </div>
        <div className="fc-grid">
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
                <p>{ this.status }</p>
              </article>
            </div>
          </div>
        </div>
        {this.subNav}
      </div>
    );
  }
}
