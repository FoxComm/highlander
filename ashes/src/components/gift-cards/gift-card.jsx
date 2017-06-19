// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from '../../lib/reason-utils';

// components
import { IndexLink, Link } from 'components/link';
import { Errors } from 'components/utils/errors';
import GiftCardCode from './gift-card-code';
import { DateTime } from 'components/utils/datetime';
import Currency from 'components/utils/currency';
import Spinner from 'components/core/spinner';
import { PageTitle } from '../section-title';
import Panel from '../panel/panel';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { Dropdown } from '../dropdown';
import PageNav from 'components/core/page-nav';
import ConfirmationModal from 'components/core/confirmation-modal';
import State, { formattedStatus } from '../common/state';

// data
import * as GiftCardActions from '../../modules/gift-cards/details';
import * as ReasonsActions from '../../modules/reasons';
import { stateTitles, stateActionTitles, getStateTransitions, typeTitles } from '../../paragons/gift-card';

// styles
import s from './gift-card.css';

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

  state = {
    errors: [],
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

    const content = React.cloneElement(this.props.children, {
      entity: {
        ...this.props.card,
        entityType: 'gift-card',
        entityId: params.giftCard,
      }
    });

    return (
      <div>
        <PageNav>
          <IndexLink to="gift-card-transactions" params={params}>Transactions</IndexLink>
          <Link to="gift-card-notes" params={params}>Notes</Link>
          <Link to="gift-card-activity-trail" params={params}>Activity Trail</Link>
        </PageNav>
        <div className="fc-gift-card-tabs">
          {content}
        </div>
      </div>
    );
  }

  @autobind
  handleConfirmChangeStatus() {
    this.props.saveGiftCardStatus(this.props.params.giftCard)
      .then(response => {
        try {
          const errors = JSON.parse(_.get(response, 'payload.[1].response.text'));

          this.setState({ errors });
        } catch (e) {
          this.setState({ errors: [] });
        }
      });
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
        id="fct-gift-card-state-dd"
        placeholder={stateTitles[state]}
        value={dropdownValue}
        onChange={this.onChangeState}
        items={transitions.map(state => [state, stateActionTitles[state]])}
        className={s.stateDropdown}
      />
    );
  }

  get changeConfirmationModal() {
    const shouldDisplay = this.props.confirmationShown && this.props.nextState !== 'canceled';

    let status = '';
    if (this.props.confirmationShown) {
      status = formattedStatus(this.props.nextState);
    }

    return (
      <ConfirmationModal
        isVisible={shouldDisplay}
        title="Change Gift Card State?"
        confirmLabel="Yes, Change State"
        onCancel={() => this.props.cancelChangeGiftCardStatus(this.props.params.giftCard)}
        onConfirm={() => this.props.saveGiftCardStatus(this.props.params.giftCard)}
      >
        Are you sure you want to change the gift card state to
        <strong className="fc-gift-card-detail__new-status">{status}</strong>?
      </ConfirmationModal>
    );
  }

  get cancellationConfirmationModal() {
    const props = this.props;
    const shouldDisplay = this.props.confirmationShown && this.props.nextState === 'canceled';

    let reasons = [];
    if (props.reasons && props.reasons[this.reasonType]) {
      reasons = _.map(props.reasons[this.reasonType], reason => [reason.id, reason.body]);
    }

    return (
      <ConfirmationModal
        isVisible={shouldDisplay}
        title="Cancel Gift Card?"
        confirmLabel="Yes, Cancel"
        onCancel={() => this.props.cancelChangeGiftCardStatus(this.props.params.giftCard)}
        onConfirm={this.handleConfirmChangeStatus}
      >
        <div>Are you sure you want to cancel this gift card?</div>
        {this.state.errors.length && <Errors errors={this.state.errors} />}
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
              value={props.reasonId}
              onChange={(reasonId) => this.props.changeCancellationReason(this.props.params.giftCard, reasonId)}
            />
          </div>
        </div>
      </ConfirmationModal>
    );
  }

  render() {
    const card = this.props.card;

    if (!card) {
      return <Spinner className={s.spinner} />;
    }

    return (
      <div className="fc-gift-card">
        <PageTitle title="Gift Card" subtitle={<GiftCardCode value={card.code} />} />
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-3">
            <Panel title="Available Balance" featured={true}>
              <Currency id="fct-panel__available-balance" value={card.availableBalance} />
            </Panel>
          </div>
        </div>
        <PanelList className="fc-grid fc-grid-collapse">
          <PanelListItem title="Original Balance">
            <Currency id="fct-panel__original-balance" value={card.originalBalance} />
          </PanelListItem>
          <PanelListItem title="Current Balance">
            <Currency id="fct-panel__original-balance" value={card.currentBalance} />
          </PanelListItem>
          <PanelListItem title="Date/Time Issued">
            <DateTime value={card.createdAt} />
          </PanelListItem>
          <PanelListItem title="Gift Card Type">
            {typeTitles[card.originType]}
          </PanelListItem>
          <PanelListItem title="Current State">
            {this.cardState}
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

                <p>
                  <strong>Recipient Cell (Optional)</strong>
                  <br />
                  {card.recipientCell ? `${card.recipientCell}` : 'None'}
                </p>
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
