// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from '../../lib/reason-utils';

// components
import { IndexLink, Link } from '../link';
import GiftCardCode from './gift-card-code';
import { DateTime } from '../common/datetime';
import Currency from '../common/currency';
import { PrimaryButton } from '../common/buttons';
import WaitAnimation from '../common/wait-animation';
import { PageTitle } from '../section-title';
import Panel from '../panel/panel';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { Dropdown } from '../dropdown';
import LocalNav from '../local-nav/local-nav';
import ConfirmationDialog from '../modal/confirmation-dialog';
import State, { formattedStatus } from '../common/state';

// data
import * as GiftCardActions from '../../modules/gift-cards/details';
import * as ReasonsActions from '../../modules/reasons';
import { states, stateTitles, stateActionTitles, getStateTransitions, typeTitles } from '../../paragons/gift-card';

@connect((state, props) => ({
  ...state.giftCards.details[props.params.giftCard],
  ...state.reasons,
}), {
  ...GiftCardActions,
  ...ReasonsActions,
})
export default class GiftCard extends React.Component {

  static propTypes = {
    card: PropTypes.shape({
      code: PropTypes.string,
      state: PropTypes.string,
      senderName: PropTypes.string,
      recipientName: PropTypes.string,
      recipientEmail: PropTypes.string,
      recipientCell: PropTypes.string,
    }),
    children: PropTypes.node,
    editGiftCard: PropTypes.func,
    confirmationShown: PropTypes.bool,
    reasons: PropTypes.object,
    reasonId: PropTypes.number,
    fetchGiftCardIfNeeded: PropTypes.func.isRequired,
    changeGiftCardStatus: PropTypes.func.isRequired,
    saveGiftCardStatus: PropTypes.func.isRequired,
    fetchReasons: PropTypes.func.isRequired,
    isFetching: PropTypes.bool,
    changeCancellationReason: PropTypes.func.isRequired,
    params: PropTypes.shape({
      giftCard: PropTypes.string.isRequired
    }).isRequired
  };

  static defaultProps = {
    confirmationShown: false
  };

  componentDidMount() {
    const { giftCard } = this.props.params;

    this.props.fetchGiftCardIfNeeded(giftCard);
    if (_.isEmpty(this.props.reasons)) {
      this.props.fetchReasons(this.reasonType);
    }
  }

  @autobind
  onChangeState(value) {
    this.props.changeGiftCardStatus(this.props.card.code, value);
  }

  get subNav() {
    const params = { giftCard: this.props.card.code };

    if (!this.props.card.code) {
      return null;
    }

    const content = React.cloneElement(this.props.children, { entity: this.props.card });

    return (
      <div>
        <LocalNav>
          <IndexLink to="gift-card-transactions" params={params}>Transactions</IndexLink>
          <Link to="gift-card-notes" params={params}>Notes</Link>
          <Link to="gift-card-activity-trail" params={params}>Activity Trail</Link>
        </LocalNav>
        <div className="fc-gift-card-tabs">
          {content}
        </div>
      </div>
    );
  }

  get reasonType() {
    return ReasonType.CANCELLATION;
  }

  get cardState() {
    const { card, nextState } = this.props;
    const { state } = card;
    const transitions = getStateTransitions(card);
    const dropdownValue = nextState ? nextState : state;

    if (!transitions.length) {
      return <State value={state} model="giftCard" />;
    }

    return (
      <Dropdown
        placeholder={stateTitles[state]}
        value={dropdownValue}
        onChange={this.onChangeState}
        items={transitions.map(state => [state, stateActionTitles[state]])}
      />
    );
  }

  get changeConfirmationModal() {
    const shouldDisplay = this.props.confirmationShown && this.props.nextState !== 'canceled';

    let status = '';
    if (this.props.confirmationShown) {
      status = formattedStatus(this.props.nextState);
    }

    const message = (
      <span>
        Are you sure you want to change the gift card state to
        <strong className="fc-gift-card-detail__new-status">{ status }</strong>
        ?
      </span>
    );
    return (
      <ConfirmationDialog
        isVisible={shouldDisplay}
        header="Change Gift Card State?"
        body={message}
        cancel="Cancel"
        confirm="Yes, Change State"
        onCancel={() => this.props.cancelChangeGiftCardStatus(this.props.params.giftCard)}
        confirmAction={() => this.props.saveGiftCardStatus(this.props.params.giftCard)}
      />
    );
  }

  get cancellationConfirmationModal() {
    const props = this.props;
    const shouldDisplay = this.props.confirmationShown && this.props.nextState === 'canceled';

    let reasons = [];
    if (props.reasons && props.reasons[this.reasonType]) {
      reasons = _.map(props.reasons[this.reasonType], reason => [reason.id, reason.body]);
    }
    const value = props.reasonId;

    const body = (
      <div>
        <div>Are you sure you want to cancel this gift card?</div>
        <div className="fc-gift-card-detail__cancel-reason">
          <div>
            <label>
              Cancel Reason
              <span className="fc-gift-card-detail__cancel-reason-asterisk">*</span>
            </label>
          </div>
          <div className="fc-gift-card-detail__cancel-reason-selector">
            <Dropdown
              name="cancellationReason"
              placeholder="- Select -"
              items={reasons}
              value={value}
              onChange={(reasonId) => this.props.changeCancellationReason(this.props.params.giftCard, reasonId)}
            />
          </div>
        </div>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={shouldDisplay}
        header="Cancel Gift Card?"
        body={body}
        cancel="Cancel"
        confirm="Yes, Cancel"
        onCancel={() => this.props.cancelChangeGiftCardStatus(this.props.params.giftCard)}
        confirmAction={() => this.props.saveGiftCardStatus(this.props.params.giftCard)}
      />
    );
  }

  render() {
    const card = this.props.card;

    if (!card) {
      return <div className="fc-gift-card-detail"><WaitAnimation /></div>;
    }

    return (
      <div className="fc-gift-card">
        <PageTitle title="Gift Card" subtitle={<GiftCardCode value={card.code} />}/>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-3">
            <Panel title="Available Balance" featured={true}>
              <Currency value={card.availableBalance} />
            </Panel>
          </div>
        </div>
        <PanelList className="fc-grid fc-grid-collapse fc-grid-md-1-5">
          <PanelListItem title="Original Balance">
            <Currency value={card.originalBalance} />
          </PanelListItem>
          <PanelListItem title="Current Balance">
            <Currency value={card.currentBalance} />
          </PanelListItem>
          <PanelListItem title="Date/Time Issued">
            <DateTime value={card.createdAt} />
          </PanelListItem>
          <PanelListItem title="Gift Card Type">
            { typeTitles[card.originType] }
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
                  {card.senderName ? `${card.senderName}` : 'None'}
                </p>

                <p><strong>Recipient</strong><br />{card.recipientName ? `${card.recipientName}` : 'None'}</p>

                <p><strong>Recipient Email</strong><br />{card.recipientEmail ? `${card.recipientEmail}` : 'None'}</p>

                <p><strong>Recipient Cell (Optional)</strong><br />{card.recipientCell ? `${card.recipientCell}` : 'None'}</p>
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
        {this.changeConfirmationModal}
        {this.cancellationConfirmationModal}
      </div>
    );
  }
}
