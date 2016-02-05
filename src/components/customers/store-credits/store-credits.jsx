
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from '../../../lib/reason-utils';
import { bindActionCreators } from 'redux';

// components
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import MultiSelectTable from '../../table/multi-select-table';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';
import Dropdown from '../../dropdown/dropdown';
import ConfirmationDialog from '../../modal/confirmation-dialog';
import { Checkbox } from '../../checkbox/checkbox';
import SearchableList from '../../list-page/searchable-list';
import StoreCreditRow from './storecredit-row';

// redux
import { actions as StoreCreditsActions } from '../../../modules/customers/store-credits';
import * as ReasonsActions from '../../../modules/reasons';
import * as StoreCreditTotalsActions from '../../../modules/customers/store-credit-totals';

const activeStateTransitions = [
  ['onHold', 'On Hold'],
  ['canceled', 'Cancel Store Credit'],
];

const onHoldStateTransitions = [
  ['active', 'Active'],
  ['canceled', 'Cancel Store Credit'],
];

const mapStateToProps = (state, props) => ({
  list: state.customers.storeCredits,
  storeCreditTotals: state.customers.storeCreditTotals[props.params.customerId],
  reasons: state.reasons,
});

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(StoreCreditsActions, dispatch),
    totalsActions: bindActionCreators(StoreCreditTotalsActions, dispatch),
    reasonsActions: bindActionCreators(ReasonsActions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class StoreCredits extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  static propTypes = {
    params: PropTypes.object,
    tableColumns: PropTypes.array,
    fetchStoreCredits: PropTypes.func,
    fetchReasons: PropTypes.func,
    cancelChange: PropTypes.func
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
        type: 'state',
        model: 'storeCredit'
      }
    ]
  };

  get customerId() {
    return this.props.params.customerId;
  }

  get reasonType() {
    return ReasonType.CANCELLATION;
  }

  get defaultSearchOptions() {
    return {
      singleSearch: true,
      initialFilters: [{
        display: 'Customer: ' + this.customerId,
        selectedTerm: 'customerId',
        selectedOperator: 'eq',
        hidden: true,
        value: {
          type: 'number',
          value: this.customerId
        }
      }],
    };
  }

  componentDidMount() {
    this.props.reasonsActions.fetchReasons(this.reasonType);
    this.props.totalsActions.fetchTotals(this.customerId);
  }

  @autobind
  renderRowState(rowId, rowState) {
    const customerId = this.customerId;
    const currentState = this.formattedState(rowState);
    switch(rowState) {
      case 'active':
        return (
          <Dropdown name="state"
                    items={ activeStateTransitions }
                    placeholder={ currentState }
                    value={ rowState }
                    onChange={ (value, title) =>
                      this.props.changeState(customerId, rowId, value) } />
        );
      case 'onHold':
        return (
          <Dropdown name="state"
                    items={ onHoldStateTransitions }
                    placeholder={ currentState }
                    value={ rowState }
                    onChange={ (value, title) =>
                      this.props.changeState(customerId, rowId, value) } />
        );
      default:
        return (<span>{rowState}</span>);
    }
  }

  renderRow(row, index, columns, params) {
    const key = `sc-transaction-${row.id}`;
    return (
      <StoreCreditRow key={key}
                      storeCredit={row}
                      columns={columns}
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
    if (this.props.storeCreditToChange) {
      state = this.formattedState(this.props.storeCreditToChange.state);
    }
    const message = (
      <span>
        Are you sure you want to change the store credit state to
        <strong className="fc-store-credit-new-state">{ state }</strong>
        ?
      </span>
    );
    const shouldDisplay = this.props.storeCreditToChange &&
      this.props.storeCreditToChange.state !== 'canceled';
    return (
      <ConfirmationDialog
          isVisible={shouldDisplay}
          header="Change Store Credit State?"
          body={message}
          cancel="Cancel"
          confirm="Yes, Change State"
          cancelAction={ () => this.props.cancelChange(this.customerId) }
          confirmAction={ () => this.props.saveStateChange(this.customerId) } />
    );
  }

  get confirmCancellation() {
    const props = this.props;

    let reasons = [];
    if (props.reasons && props.reasons[this.reasonType]) {
      reasons = _.map(props.reasons[this.reasonType], reason => [reason.id, reason.body]);
    }
    const value = props.storeCreditToChange && props.storeCreditToChange.reasonId;

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
                      onChange={(value) => props.reasonChange(this.customerId, value)} />
          </div>
        </div>
      </div>
    );
    const shouldDisplay = props.storeCreditToChange && props.storeCreditToChange.state === 'canceled';

    return (
      <ConfirmationDialog
          isVisible={ shouldDisplay }
          header="Cancel Store Credit?"
          body={ body }
          cancel="Cancel"
          confirm="Yes, Cancel"
          cancelAction={ () => props.cancelChange(this.customerId) }
          confirmAction={ () => props.saveStateChange(this.customerId) } />
    );
  }

  render() {
    const props = this.props;
    const totals = _.get(props, ['storeCreditTotals', 'totals'], {});

    return (
      <div className="fc-store-credits fc-list-page">
        <Summary totals={totals}
                 params={props.params}
                 history={this.context.history}
                 transactionsSelected={false} />
        <div className="fc-grid fc-list-page-content fc-store-credits__list">
          <SearchableList
            title="Store Credits"
            emptyResultMessage="No store credits found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={this.props.tableColumns}
            searchActions={this.props.actions}
            searchOptions={this.defaultSearchOptions} />
        </div>
        { this.confirmStateChange }
        { this.confirmCancellation }
      </div>
    );
  }
}
