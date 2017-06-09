// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from '../../lib/reason-utils';

// components
import { IndexLink, Link } from 'components/link';
import ContentTypeCode from './content-type-code';
import { DateTime } from '../common/datetime';
import Currency from '../common/currency';
import WaitAnimation from '../common/wait-animation';
import { PageTitle } from '../section-title';
import Panel from '../panel/panel';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { Dropdown } from '../dropdown';
import PageNav from 'components/core/page-nav';
import ConfirmationDialog from '../modal/confirmation-dialog';
import State, { formattedStatus } from '../common/state';

// data
import * as ContentTypeActions from '../../modules/content-types/details';
import * as ReasonsActions from '../../modules/reasons';
import { stateTitles, stateActionTitles, getStateTransitions, typeTitles } from '../../paragons/content-type';

@connect((state, props) => ({
  ...state.contentTypes.details[props.params.contentType],
  ...state.reasons,
}), {
  ...ContentTypeActions,
  ...ReasonsActions,
})
export default class ContentType extends React.Component {

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
    editContentType: PropTypes.func,
    confirmationShown: PropTypes.bool,
    reasons: PropTypes.object,
    reasonId: PropTypes.number,
    fetchContentTypeIfNeeded: PropTypes.func.isRequired,
    changeContentTypeStatus: PropTypes.func.isRequired,
    saveContentTypeStatus: PropTypes.func.isRequired,
    fetchReasons: PropTypes.func.isRequired,
    isFetching: PropTypes.bool,
    changeCancellationReason: PropTypes.func.isRequired,
    params: PropTypes.shape({
      contentType: PropTypes.string.isRequired
    }).isRequired
  };

  static defaultProps = {
    confirmationShown: false
  };

  componentDidMount() {
    const { contentType } = this.props.params;

    this.props.fetchContentTypeIfNeeded(contentType);
    if (_.isEmpty(this.props.reasons)) {
      this.props.fetchReasons(this.reasonType);
    }
  }

  @autobind
  onChangeState(value) {
    this.props.changeContentTypeStatus(this.props.card.code, value);
  }

  get subNav() {
    const params = { contentType: this.props.card.code };

    if (!this.props.card.code) {
      return null;
    }

    const content = React.cloneElement(this.props.children, {
      entity: {
        ...this.props.card,
        entityType: 'content-type',
        entityId: params.contentType,
      }
    });

    return (
      <div>
        <PageNav>
          <IndexLink to="content-type-transactions" params={params}>Transactions</IndexLink>
          <Link to="content-type-notes" params={params}>Notes</Link>
          <Link to="content-type-activity-trail" params={params}>Activity Trail</Link>
        </PageNav>
        <div className="fc-content-type-tabs">
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
      return <State value={state} model="contentType" />;
    }

    return (
      <Dropdown
        id="fct-content-type-state-dd"
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
        Are you sure you want to change the content type state to
        <strong className="fc-content-type-detail__new-status">{ status }</strong>
        ?
      </span>
    );
    return (
      <ConfirmationDialog
        isVisible={shouldDisplay}
        header="Change Content Type State?"
        body={message}
        cancel="Cancel"
        confirm="Yes, Change State"
        onCancel={() => this.props.cancelChangeContentTypeStatus(this.props.params.contentType)}
        confirmAction={() => this.props.saveContentTypeStatus(this.props.params.contentType)}
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
        <div>Are you sure you want to cancel this content type?</div>
        <div className="fc-content-type-detail__cancel-reason">
          <div>
            <label>
              Cancel Reason
              <span className="fc-content-type-detail__cancel-reason-asterisk">*</span>
            </label>
          </div>
          <div className="fc-content-type-detail__cancel-reason-selector">
            <Dropdown
              name="cancellationReason"
              placeholder="- Select -"
              items={reasons}
              value={value}
              onChange={(reasonId) => this.props.changeCancellationReason(this.props.params.contentType, reasonId)}
            />
          </div>
        </div>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={shouldDisplay}
        header="Cancel Content Type?"
        body={body}
        cancel="Cancel"
        confirm="Yes, Cancel"
        onCancel={() => this.props.cancelChangeContentTypeStatus(this.props.params.contentType)}
        confirmAction={() => this.props.saveContentTypeStatus(this.props.params.contentType)}
      />
    );
  }

  render() {
    const card = this.props.card;

    if (!card) {
      return <div className="fc-content-type-detail"><WaitAnimation /></div>;
    }

    return (
      <div className="fc-content-type">
        <PageTitle title="Content Type" subtitle={<ContentTypeCode value={card.code} />} />
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-3">
            <Panel title="Available Balance" featured={true}>
              <Currency id="fct-panel__available-balance" value={card.availableBalance} />
            </Panel>
          </div>
        </div>
        <PanelList className="fc-grid fc-grid-collapse fc-grid-md-1-5">
          <PanelListItem title="Original Balance">
            <Currency id="fct-panel__original-balance" value={card.originalBalance} />
          </PanelListItem>
          <PanelListItem title="Current Balance">
            <Currency id="fct-panel__original-balance" value={card.currentBalance} />
          </PanelListItem>
          <PanelListItem title="Date/Time Issued">
            <DateTime value={card.createdAt} />
          </PanelListItem>
          <PanelListItem title="Content Type Type">
            { typeTitles[card.originType] }
          </PanelListItem>
          <PanelListItem title="Current State">
            { this.cardState }
          </PanelListItem>
        </PanelList>
        <div className="fc-grid fc-grid-md-1-1 fc-grid-collapse fc-panel fc-content-type-detail-message">
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
