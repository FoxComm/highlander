import React, { PropTypes } from 'react';
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';
import Dropdown from '../../dropdown/dropdown';
import ConfirmationDialog from '../../modal/confirmation-dialog';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as StoreCreditsActions from '../../../modules/customers/store-credits';
import * as ReasonsActions from '../../../modules/reasons';

const activeStateTransitions = {
  'onHold': 'On Hold',
  'canceled': 'Cancele Store Credit'
};

const onHoldStateTransitions = {
  'active': 'Active',
  'canceled': 'Cancele Store Credit'
};

const actions = {
  ...StoreCreditsActions,
  ...ReasonsActions
};

@connect((state, props) => ({
  ...state.customers.storeCredits[props.params.customerId],
  ...state.reasons
}), actions)
export default class StoreCredits extends React.Component {

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
        text: 'Availabale Balance'
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

  componentDidMount() {
    this.props.fetchStoreCredits(this.entityType(this.customerId));
    this.props.fetchReasons();
  }

  entityType(customerId) {
    return {entityType: 'storeCredits', entityId: customerId};
  }

  @autobind
  renderRowState(rowId, rowState) {
    const customerId = this.customerId;
    const currentStatus = rowState.charAt(0).toUpperCase() + rowState.slice(1);
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
        return (<span>rowState</span>);
    }
  }

  @autobind
  renderRow(row) {
    return (
      <TableRow key={`storeCredits-row-${row.id}`}>
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

  get confirmStatusChange() {
    const message = (
      <span>
        Are you sure you want to change the gift card state to
        <strong>{this.props.storeCreditToChange && this.props.storeCreditToChange.status}</strong>
        ?
      </span>
    );
    const shouldDisplay = this.props.storeCreditToChange &&
      this.props.storeCreditToChange.status !== 'canceled';
    return (
      <ConfirmationDialog
          isVisible={ shouldDisplay }
          header='Change Store Credit State?'
          body={ message }
          cancel='Cancel'
          confirm='Yes, Change State'
          cancelAction={ () => this.props.cancelChange(this.customerId) }
          confirmAction={ () => console.log('confirm not implemented') } />
    );
  }

  get confirmCancellation() {
    const body = 'Are you sure you want to cancel this store credit?';
    const shouldDisplay = this.props.storeCreditToChange &&
      this.props.storeCreditToChange.status === 'canceled';
    return (
      <ConfirmationDialog
          isVisible={ shouldDisplay }
          header='Cancel Store Credit?'
          body={ body }
          cancel='Cancel'
          confirm='Yes, Cancel'
          cancelAction={ () => this.props.cancelChange(this.customerId) }
          confirmAction={ () => console.log('confirm not implemented') } />
    );
  }

  render() {
    const props = this.props;
    return (
      <div className="fc-store-credits fc-list-page">
        <Summary {...props} />
        <div className="fc-grid fc-list-page-content">
          <SearchBar />
          <div className="fc-col-md-1-1">
            <TableView
              columns={props.tableColumns}
              data={props.storeCredits}
              renderRow={this.renderRow}
              setState={props.setFetchParams}
              />
          </div>
        </div>
        { this.confirmStatusChange }
        { this.confirmCancellation }
      </div>
    );
  }
}
