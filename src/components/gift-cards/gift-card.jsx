import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import { IndexLink, Link } from '../link';
import GiftCardCode from './gift-card-code';
import { DateTime } from '../common/datetime';
import Currency from '../common/currency';
import { PrimaryButton } from '../common/buttons';
import { PageTitle } from '../section-title';
import Panel from '../panel/panel';
import {PanelList, PanelListItem} from '../panel/panel-list';
import { Dropdown, DropdownItem } from '../dropdown';
import LocalNav from '../local-nav/local-nav';

import * as GiftCardActions from '../../modules/gift-cards/details';

@connect((state, props) => ({
  ...state.giftCards.details[props.params.giftCard]
}), GiftCardActions)
export default class GiftCard extends React.Component {

  static propTypes = {
    card: PropTypes.shape({
      code: PropTypes.string,
      state: PropTypes.string
    }),
    children: PropTypes.node,
    editGiftCard: PropTypes.func,
    fetchGiftCardIfNeeded: PropTypes.func.isRequired,
    params: PropTypes.shape({
      giftCard: PropTypes.string.isRequired
    }).isRequired
  };

  componentDidMount() {
    let { giftCard } = this.props.params;

    this.props.fetchGiftCardIfNeeded(giftCard);
  }

  @autobind
  onChangeState({target}) {
    this.props.editGiftCard(this.props.card.code, {state: target.value});
  }

  @autobind
  resendGiftCard() {
    console.log('Resend');
  }

  get subNav() {
    const params = {giftCard: this.props.card.code};

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

  get cardState() {
    const {state} = this.props.card;

    if (state === 'Canceled') {
      return <span>{state}</span>;
    } else {
      return (
        <Dropdown onChange={this.onChangeState} value={state}>
          <DropdownItem value="active">Active</DropdownItem>
          <DropdownItem value="onHold">On Hold</DropdownItem>
          <DropdownItem value="canceled">Cancel Gift Card</DropdownItem>
        </Dropdown>
      );
    }
  }

  render() {
    let card = this.props.card;

    if (!card) {
      return <div className="fc-gift-card-detail"></div>;
    }

    return (
      <div className="fc-gift-card">
        <PageTitle title="Gift Card" subtitle={<GiftCardCode value={card.code} />}>
          <PrimaryButton onClick={this.resendGiftCard}>Resend Gift Card</PrimaryButton>
        </PageTitle>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-3">
            <Panel title="Available Balance" featured={true}>
              <Currency value={card.availableBalance}/>
            </Panel>
          </div>
        </div>
        <PanelList className="fc-grid fc-grid-collapse fc-grid-md-1-5">
          <PanelListItem title="Original Balance">
            <Currency value={card.originalBalance}/>
          </PanelListItem>
          <PanelListItem title="Current Balance">
            <Currency value={card.currentBalance}/>
          </PanelListItem>
          <PanelListItem title="Date/Time Issued">
            <DateTime value={card.createdAt}/>
          </PanelListItem>
          <PanelListItem title="Gift Card Type">
            { card.originType }
          </PanelListItem>
          <PanelListItem title="Current State">
            { this.cardState }
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
