
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

// const actions = {
//   ...StoreCreditsActions,
//   ...ReasonsActions,
//   ...StoreCreditTotalsActions
// };

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
        field: 'type',
        text: 'Type'
      },
      {
        field: 'issuedBy',
        text: 'Issued By'
      },
      {
        field: 'originalBalance',
        text: 'Original Balance'
      },
      {
        field: 'currentBalance',
        text: 'Current Balance'
      },
      {
        field: 'availableBalance',
        text: 'Available Balance'
      },
      {
        field: 'state',
        text: 'State'
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
        display: "Customer: " + this.customerId,
        selectedTerm: "customer.customerId",
        selectedOperator: "eq",
        hidden: true,
        value: {
          type: "string",
          value: '' + this.customerId
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
    const currentStatus = this.formattedStatus(rowState);
    switch(rowState) {
      case 'active':
        return (
          <Dropdown name="status"
                    items={ activeStateTransitions }
                    placeholder={ currentStatus }
                    value={ rowState }
                    onChange={ (value, title) =>
                      this.props.changeStatus(customerId, rowId, value) } />
        );
      case 'onHold':
        return (
          <Dropdown name="status"
                    items={ onHoldStateTransitions }
                    placeholder={ currentStatus }
                    value={ rowState }
                    onChange={ (value, title) =>
                      this.props.changeStatus(customerId, rowId, value) } />
        );
      default:
        return (<span>{rowState}</span>);
    }
  }

  @autobind
  renderRow(row) {
    return (
      <TableRow key={`storeCredits-row-${row.id}`}>
        <TableCell><Checkbox /></TableCell>
        <TableCell><DateTime value={ row.createdAt }/></TableCell>
        <TableCell>{ row.id }</TableCell>
        <TableCell>{ row.originType }</TableCell>
        <TableCell>{ /* store credit, no data for it too */ }</TableCell>
        <TableCell><Currency value={ row.originalBalance } /></TableCell>
        <TableCell><Currency value={ row.currentBalance } /></TableCell>
        <TableCell><Currency value={ row.availableBalance } /></TableCell>
        <TableCell>{ this.renderRowState(row.id, row.status) }</TableCell>
      </TableRow>
    );
  }

  formattedStatus(status) {
    switch (status) {
      case 'onHold':
        return 'On Hold';
      case 'active':
        return 'Active';
      default:
        return status;
    }
  }

  get confirmStatusChange() {
    let status = '';
    if (this.props.storeCreditToChange) {
      status = this.formattedStatus(this.props.storeCreditToChange.status);
    }
    const message = (
      <span>
        Are you sure you want to change the store credit state to
        <strong className="fc-store-credit-new-status">{ status }</strong>
        ?
      </span>
    );
    const shouldDisplay = this.props.storeCreditToChange &&
      this.props.storeCreditToChange.status !== 'canceled';
    return (
      <ConfirmationDialog
          isVisible={shouldDisplay}
          header="Change Store Credit State?"
          body={message}
          cancel="Cancel"
          confirm="Yes, Change State"
          cancelAction={ () => this.props.cancelChange(this.customerId) }
          confirmAction={ () => this.props.saveStatusChange(this.customerId) } />
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
    const shouldDisplay = props.storeCreditToChange && props.storeCreditToChange.status === 'canceled';

    return (
      <ConfirmationDialog
          isVisible={ shouldDisplay }
          header="Cancel Store Credit?"
          body={ body }
          cancel="Cancel"
          confirm="Yes, Cancel"
          cancelAction={ () => props.cancelChange(this.customerId) }
          confirmAction={ () => props.saveStatusChange(this.customerId) } />
    );
  }

  render() {
    const props = this.props;
    const totals = _.get(props, ['storeCreditTotals', 'totals'], {});
    console.log(this.props);

    return (
      <div className="fc-store-credits fc-list-page">
        <Summary totals={totals}
                 params={props.params}
                 history={this.context.history}
                 transactionsSelected={false} />
        <div className="fc-grid fc-list-page-content">
          <SearchableList
            title="Store Credits"
            emptyResultMessage="No store credits found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={this.props.tableColumns}
            searchActions={this.props.actions}
            searchOptions={this.defaultSearchOptions} />
        </div>
        { this.confirmStatusChange }
        { this.confirmCancellation }
      </div>
    );
  }
}
