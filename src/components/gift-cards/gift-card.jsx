'use strict';

import React, { PropTypes } from 'react';
import { IndexLink, Link } from '../link';
import { formatCurrency } from '../../lib/format';
import moment from 'moment';
import GiftCardStore from '../../stores/gift-cards';
import GiftCardActions from '../../actions/gift-cards';
import SectionTitle from '../section-title/section-title';
import Panel from '../panel/panel';
import LocalNav from '../local-nav/local-nav';

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
      <div>
        <LocalNav>
          <IndexLink to="gift-card-transactions" params={params}>Transactions</IndexLink>
          <Link to="gift-card-notes" params={params}>Notes</Link>
          <Link to="gift-card-activity-trail" params={params}>Activity Trail</Link>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            {content}
          </div>
        </div>
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
      return <div className="fc-gift-card-detail"></div>;
    }

    return (
      <div>
        <SectionTitle title="Gift Card" subtitle={this.giftCard.code}>
          <button onClick={this.resendGiftCard.bind(this)} className="fc-btn fc-btn-primary">Resend Gift Card</button>
        </SectionTitle>
        <div className="fc-grid">
          <div className="fc-col-md-1-3">
            <Panel title="Available Balance" featured={true}>
              { formatCurrency(card.availableBalance) }
            </Panel>
          </div>
        </div>
        <div className="fc-grid fc-grid-md-1-5 fc-grid-collapse fc-panel-list">
          <div className="fc-panel-list-panel">
            <header className="fc-panel-list-header">Original Balance</header>
            <p className="fc-panel-list-content">{ formatCurrency(card.originalBalance) }</p>
          </div>
          <div className="fc-panel-list-panel">
            <header className="fc-panel-list-header">Current Balance</header>
            <p className="fc-panel-list-content">{ formatCurrency(card.currentBalance) }</p>
          </div>
          <div className="fc-panel-list-panel">
            <header className="fc-panel-list-header">Date/Time Issued</header>
            <p className="fc-panel-list-content">{ moment(card.date).format('L LTS') }</p>
          </div>
          <div className="fc-panel-list-panel">
            <header className="fc-panel-list-header">Gift Card Type</header>
            <p className="fc-panel-list-content">{ card.originType }</p>
          </div>
          <div className="fc-panel-list-panel">
            <header className="fc-panel-list-header">Current State</header>
            <p className="fc-panel-list-content">{ this.status }</p>
          </div>
        </div>
        <div className="fc-grid fc-grid-md-1-1 fc-grid-collapse fc-panel fc-gift-card-detail-message">
          <div>
            <div className="fc-grid">
              <div className="fc-col-md-1-3">
                <p>
                  <strong>Created By</strong><br />
                  {card.storeAdmin ? `${card.storeAdmin.firstName} ${card.storeAdmin.lastName}` : 'None'}
                </p>

                <p><strong>Recipient</strong><br />None</p>

                <p><strong>Recipient Email</strong><br />None</p>

                <p><strong>Recipient Cell (Optional)</strong><br />None</p>
              </div>
              <div className="fc-col-md-2-3">
                <p><strong>Message (optional)</strong></p>

                <p>
                  {card.message}
                </p>
              </div>
            </div>
          </div>
        </div>
        {this.subNav}
      </div>
    );
  }
}
