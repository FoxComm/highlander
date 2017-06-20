/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from 'lib/reason-utils';
import { bindActionCreators } from 'redux';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { stateTitles } from 'paragons/store-credit';
import { actions as StoreCreditsActions } from 'modules/customers/store-credits';
import { actions as bulkActions } from 'modules/customers/store-credit-bulk';
import * as ReasonsActions from 'modules/reasons';
import * as StoreCreditTotalsActions from 'modules/customers/store-credit-totals';
import * as StoreCreditStateActions from 'modules/customers/store-credit-states';
import { bulkExport } from 'modules/bulk-export/bulk-export';

// components
import Summary from './summary';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { ChangeStateModal, CancelModal } from 'components/bulk-actions/modal';
import Dropdown from 'components/dropdown/dropdown';
import ConfirmationModal from 'components/core/confirmation-modal';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import StoreCreditRow from './storecredit-row';

type Props = {
  params: Object,
  actions: Object,
  list: Object,
  tableColumns: Array<Object>,
  states: {
    storeCreditToChange: {
      state: string,
    }
  },
  reasons: Object,
  storeCreditTotals: Object,
  reasonsActions: {
    fetchReasons: (reasonType: string) => Promise<*>,
  },
  totalsActions: {
    fetchTotals: (customerId: number) => Promise<*>,
  },
  stateActions: {
    cancelChange: (customerId: number) => void,
    changeState: (customerId: number, rowId: number, targetState: string) => Array<any>,
    saveStateChange: (customerId: number) => Promise<*>,
    reasonChange: (customerId: number, reasonId: number) => Array<number>,
  },
  bulkActions: {
    cancelStoreCredits: (ids: Array<number>, reasonId: number) => Promise<*>,
    changeStoreCreditsState: (ids: Array<number>, state: string) => Promise<*>,
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
};

class StoreCredits extends Component {
  props: Props;

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
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const customerId = this.customerId;
    const key = `sc-transaction-${row.id}`;

    return (
      <StoreCreditRow
        storeCredit={row}
        columns={columns}
        changeState={(rowId, value) => this.props.stateActions.changeState(customerId, rowId, value)}
        key={key}
        params={params}
      />
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

  get confirmationState() {
    const { states } = this.props;

    if (!states || !states.storeCreditToChange) return '';

    return this.formattedState(states.storeCreditToChange.state);
  }

  get confirmationMessage() {
    return (
      <span>
        Are you sure you want to change the store credit state to
        <strong className="fc-store-credit-new-state">{ this.confirmationState }</strong>
        ?
      </span>
    );
  }

  get confirmStateChange() {
    const { states, stateActions } = this.props;

    const shouldDisplay = states && states.storeCreditToChange && states.storeCreditToChange.state !== 'canceled';

    return (
      <ConfirmationModal
        isVisible={shouldDisplay}
        title="Change Store Credit State?"
        confirmLabel="Yes, Change State"
        onConfirm={ () => stateActions.saveStateChange(this.customerId) }
        onCancel={ () => stateActions.cancelChange(this.customerId) }
      >
        {this.confirmationMessage}
      </ConfirmationModal>
    );
  }

  get reasons() {
    const rawReasons = _.get(this.props, ['reasons', 'reasons', this.reasonType]);

    if (_.isEmpty(rawReasons)) return [];

    return _.map(rawReasons, reason => [reason.id, reason.body]);
  }

  get confirmationBody() {
    const value = _.get(this.props, ['states', 'storeCreditToChange', 'reasonId']);

    return (
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
                      items={this.reasons}
                      value={value}
                      onChange={(value) => this.props.stateActions.reasonChange(this.customerId, value)} />
          </div>
        </div>
      </div>
    );
  }

  get confirmCancellation() {
    const { props } = this;

    const shouldDisplay = _.isEqual(_.get(props, ['states', 'storeCreditToChange', 'state']), 'canceled');

    return (
      <ConfirmationModal
        isVisible={shouldDisplay}
        title="Cancel Store Credit?"
        confirmLabel="Yes, Cancel"
        onConfirm={ () => props.stateActions.saveStateChange(this.customerId) }
        onCancel={ () => props.stateActions.cancelChange(this.customerId) }
      >
        {this.confirmationBody}
      </ConfirmationModal>
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
        }}
      />
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
          onConfirm={() => changeStoreCreditsState(toggledIds, state)}
        />
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

  get cancelAction() {
    return [
      'Cancel Store Credits',
      this.cancelStoreCredits,
      'successfully canceled',
      'could not be canceled',
    ];
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const { tableColumns } = this.props;
    const modalTitle = 'Store Credits';
    const entity = 'storeCredits';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions() {
    return [
      bulkExportBulkAction(this.bulkExport, 'Store Credits'),
      this.cancelAction,
      this.getChangeStoreCreditsStateAction('active'),
      this.getChangeStoreCreditsStateAction('onHold'),
    ];
  }

  renderDetail(messages, id) {
    return <span key={id}>Store Credit #{id}</span>;
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
            actions={this.bulkActions}
          >
            <SelectableSearchList
              exportEntity="storeCredits"
              exportTitle="Store Credits"
              bulkExport
              bulkExportAction={this.props.bulkExportAction}
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

const mapStateToProps = (state, props) => {
  return {
    list: _.get(state.customers, 'storeCredits', {}),
    storeCreditTotals: _.get(state.customers, `storeCreditTotals[${props.params.customerId}]`, {}),
    reasons: _.get(state, 'reasons', {}),
    states: _.get(state.customers, `storeCreditStates[${props.params.customerId}]`, {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(StoreCreditsActions, dispatch),
    totalsActions: bindActionCreators(StoreCreditTotalsActions, dispatch),
    reasonsActions: bindActionCreators(ReasonsActions, dispatch),
    stateActions: bindActionCreators(StoreCreditStateActions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(StoreCredits);
