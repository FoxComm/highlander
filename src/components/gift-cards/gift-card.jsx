'use strict';

import React, { PropTypes } from 'react';
import { IndexLink, Link } from '../link';
import { autobind } from 'core-decorators';
import { formatCurrency } from '../../lib/format';
import { connect } from 'react-redux';
import * as GiftCardActions from '../../modules/gift-cards/details';
import moment from 'moment';
import SectionTitle from '../section-title/section-title';
import Panel from '../panel/panel';
import {PanelList, PanelListItem} from '../panel/panel-list';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import LocalNav from '../local-nav/local-nav';

@connect((state, props) => ({
  ...state.giftCards.details[props.params.giftcard]
}), GiftCardActions)
export default class GiftCard extends React.Component {

  static propTypes = {
    card: PropTypes.shape({
      code: PropTypes.string
    }),
    children: PropTypes.node,
    editGiftCard: PropTypes.func,
    fetchGiftCardIfNeeded: PropTypes.func.isRequired,
    params: PropTypes.shape({
      giftcard: PropTypes.string.isRequired
    }).isRequired
  };

  componentDidMount() {
    let { giftcard } = this.props.params;

    this.props.fetchGiftCardIfNeeded(giftcard);
  }

  @autobind
  onChangeState({target}) {
    this.props.editGiftCard(this.props.card.code, {status: target.value});
  }

  get subNav() {
    const params = {giftcard: this.props.card.code};

    if (!this.props.card.code) {
      return null;
    }

    const content = React.cloneElement(this.props.children, {entity: this.props.card });

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

  get cardStatus() {
    const {status} = this.props.card;

    if (status === 'Canceled') {
      return <span>{status}</span>;
    } else {
      return (
        <Dropdown onChange={this.onChangeState} value={status}>
          <DropdownItem value="active">Active</DropdownItem>
          <DropdownItem value="onHold">On Hold</DropdownItem>
          <DropdownItem value="canceled">Cancel Gift Card</DropdownItem>
        </Dropdown>
      );
    }
  }

  resendGiftCard() {
    console.log('Resend');
  }

  render() {
    let card = this.props.card;

    if (!card) {
      return <div className="fc-gift-card-detail"></div>;
    }

    return (
      <div>
        <SectionTitle title="Gift Card" subtitle={card.code}>
          <button onClick={this.resendGiftCard.bind(this)} className="fc-btn fc-btn-primary">Resend Gift Card</button>
        </SectionTitle>
        <div className="fc-grid">
          <div className="fc-col-md-1-3">
            <Panel title="Available Balance" featured={true}>
              { formatCurrency(card.availableBalance) }
            </Panel>
          </div>
        </div>
        <PanelList className="fc-grid fc-grid-collapse fc-grid-md-1-5">
          <PanelListItem title="Original Balance">
            { formatCurrency(card.originalBalance) }
          </PanelListItem>
          <PanelListItem title="Current Balance">
            { formatCurrency(card.currentBalance) }
          </PanelListItem>
          <PanelListItem title="Date/Time Issued">
            { moment(card.date).format('L LTS') }
          </PanelListItem>
          <PanelListItem title="Gift Card Type">
            { card.originType }
          </PanelListItem>
          <PanelListItem title="Current State">
            { this.cardStatus }
          </PanelListItem>
        </PanelList>
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
