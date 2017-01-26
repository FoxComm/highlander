// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from '../../../lib/reason-utils';
import { bindActionCreators } from 'redux';

// components
import Summary from './summary';
import BulkActions from '../../bulk-actions/bulk-actions';
import BulkMessages from '../../bulk-actions/bulk-messages';
import { ChangeStateModal, CancelModal } from '../../bulk-actions/modal';
import Dropdown from '../../dropdown/dropdown';
import ConfirmationDialog from '../../modal/confirmation-dialog';
import SelectableSearchList from '../../list-page/selectable-search-list';
import StoreCreditRow from './storecredit-row';

// data
import { stateTitles } from '../../../paragons/store-credit';
import { actions as StoreCreditsActions } from '../../../modules/customers/store-credits';
import { actions as bulkActions } from '../../../modules/customers/store-credit-bulk';
import * as ReasonsActions from '../../../modules/reasons';
import * as StoreCreditTotalsActions from '../../../modules/customers/store-credit-totals';
import * as StoreCreditStateActions from '../../../modules/customers/store-credit-states';

const mapStateToProps = (state, props) => ({
  list: state.customers.storeCredits,
  storeCreditTotals: state.customers.storeCreditTotals[props.params.customerId],
  reasons: state.reasons,
  states: state.customers.storeCreditStates[props.params.customerId],
});

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(StoreCreditsActions, dispatch),
    totalsActions: bindActionCreators(StoreCreditTotalsActions, dispatch),
    reasonsActions: bindActionCreators(ReasonsActions, dispatch),
    stateActions: bindActionCreators(StoreCreditStateActions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class StoreCredits extends React.Component {

  static propTypes = {
    params: PropTypes.object,
    actions: PropTypes.object,
    list: PropTypes.object,
    tableColumns: PropTypes.array,
    reasonsActions: PropTypes.shape({
      fetchReasons: PropTypes.func,
    }),
    totalsActions: PropTypes.shape({
      fetchTotals: PropTypes.func,
    }),
    storeCreditToChange: PropTypes.shape({
      state: PropTypes.string,
    }),
    states: PropTypes.shape({
      storeCreditToChange: PropTypes.object,
    }),
    stateActions: PropTypes.shape({
      cancelChange: PropTypes.func,
      changeState: PropTypes.func,
      saveStateChange: PropTypes.func,
    }),
    bulkActions: PropTypes.shape({
      cancelStoreCredits: PropTypes.func,
      changeStoreCreditsState: PropTypes.func,
    }),
  };

  static defaultProps = {
    tableColumns: [
      {
        field: 'createdAt',
        text: 'Date/Time Issued',
        type: 'date'
      },
      {
        field: 'id',
        text: 'Store Credit Id'
      },
      {
        field: 'originType',
        text: 'Type'
      },
      {
        field: 'issuedBy',
        text: 'Issued By'
      },
      {
        field: 'originalBalance',
        text: 'Original Balance',
        type: 'currency'
      },
      {
        field: 'currentBalance',
        text: 'Current Balance',
        type: 'currency'
      },
      {
        field: 'availableBalance',
        text: 'Available Balance',
        type: 'currency'
      },
      {
        field: 'state',
        text: 'State',
      }
    ]
  };

  get customerId() {
    return this.props.params.customerId;
  }

  get reasonType() {
    return ReasonType.CANCELLATION;
  }

  componentDidMount() {
    this.props.actions.setExtraFilters([
      { term: { 'accountId': this.customerId } }
    ]);
    this.props.reasonsActions.fetchReasons(this.reasonType);
    this.props.totalsActions.fetchTotals(this.customerId);
    this.props.actions.fetch();
  }


  @autobind
  renderRow(row, index, columns, params) {
    const customerId = this.customerId;
    const key = `sc-transaction-${row.id}`;
    return (
      <StoreCreditRow
        storeCredit={row}
        columns={columns}
        changeState={(rowId, value) => this.props.stateActions.changeState(customerId, rowId, value)}
        key={key}
        params={params} />
    );
  }

  formattedState(state) {
    switch (state) {
      case 'onHold':
        return 'On Hold';
      case 'active':
        return 'Active';
      default:
        return state;
    }
  }

  get confirmStateChange() {
    let state = '';
    if (this.props.states && this.props.states.storeCreditToChange) {
      state = this.formattedState(this.props.states.storeCreditToChange.state);
    }
    const message = (
      <span>
        Are you sure you want to change the store credit state to
        <strong className="fc-store-credit-new-state">{ state }</strong>
        ?
      </span>
    );
    const shouldDisplay = this.props.states && this.props.states.storeCreditToChange &&
      this.props.states.storeCreditToChange.state !== 'canceled';
    return (
      <ConfirmationDialog
        isVisible={shouldDisplay}
        header="Change Store Credit State?"
        body={message}
        cancel="Cancel"
        confirm="Yes, Change State"
        onCancel={ () => this.props.stateActions.cancelChange(this.customerId) }
        confirmAction={ () => this.props.stateActions.saveStateChange(this.customerId) } />
    );
  }

  get confirmCancellation() {
    const props = this.props;

    const rawReasons = _.get(props, ['reasons', 'reasons', this.reasonType]);

    let reasons = [];
    if (!_.isEmpty(rawReasons)) {
      reasons = _.map(rawReasons, reason => [reason.id, reason.body]);
    }
    const value = _.get(props, ['states', 'storeCreditToChange', 'reasonId']);

    const body = (
      <div>
        <div>Are you sure you want to cancel this store credit?</div>
        <div className="fc-store-credit-cancel-reason">
          <div>
            <label>
              Cancel Reason
              <span className="fc-store-credit-cancel-reason-asterisk">*</span>
            </label>
          </div>
          <div className="fc-store-credit-cancel-reason-selector">
            <Dropdown name="cancellationReason"
                      placeholder="- Select -"
                      items={reasons}
                      value={value}
                      onChange={(value) => props.stateActions.reasonChange(this.customerId, value)} />
          </div>
        </div>
      </div>
    );
    const shouldDisplay = _.isEqual(_.get(props, ['states', 'storeCreditToChange', 'state']), 'canceled');

    return (
      <ConfirmationDialog
        isVisible={ shouldDisplay }
        header="Cancel Store Credit?"
        body={ body }
        cancel="Cancel"
        confirm="Yes, Cancel"
        onCancel={ () => props.stateActions.cancelChange(this.customerId) }
        confirmAction={ () => props.stateActions.saveStateChange(this.customerId) } />
    );
  }

  @autobind
  cancelStoreCredits(allChecked, toggledIds) {
    const { cancelStoreCredits } = this.props.bulkActions;

    return (
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => {
          cancelStoreCredits(toggledIds, reasonId);
        }} />
    );
  }

  getChangeStoreCreditsState(state) {
    const stateTitle = stateTitles[state];

    return (allChecked, toggledIds) => {
      const { changeStoreCreditsState } = this.props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => changeStoreCreditsState(toggledIds, state)} />
      );
    };
  }

  getChangeStoreCreditsStateAction(state) {
    const stateTitle = stateTitles[state];

    return [
      `Change Store Credits state to ${stateTitle}`,
      this.getChangeStoreCreditsState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  get bulkActions() {
    return [
      ['Cancel Store Credits', this.cancelStoreCredits, 'successfully canceled', 'could not be canceled'],
      this.getChangeStoreCreditsStateAction('active'),
      this.getChangeStoreCreditsStateAction('onHold'),
    ];
  }

  renderDetail(messages, id) {
    return <span key={id}>Store credit #{id}</span>;
  }

  render() {
    const props = this.props;
    const totals = _.get(props, ['storeCreditTotals', 'totals'], {});

    return (
      <div className="fc-store-credits">
        <Summary
          totals={totals}
          params={props.params}
          transactionsSelected={false}
        >
          <BulkMessages
            storePath="customers.storeCreditBulk"
            module="customers.store-credits"
            entity="store credit"
            renderDetail={this.renderDetail}
          />
        </Summary>
        <div className="fc-store-credits__list">
          <BulkActions
            module="customers.store-credits"
            entity="store credit"
            actions={this.bulkActions}>
            <SelectableSearchList
              entity="customers.storeCredits"
              title="Store Credits"
              emptyMessage="No store credits found."
              list={this.props.list}
              renderRow={this.renderRow}
              tableColumns={this.props.tableColumns}
              searchActions={this.props.actions}
              searchOptions={{singleSearch: true}}
            />
          </BulkActions>
        </div>
        { this.confirmStateChange }
        { this.confirmCancellation }
      </div>
    );
  }
}
