import React, { PropTypes } from 'react';
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';
import Dropdown from '../../dropdown/dropdown';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as StoreCreditsActions from '../../../modules/customers/store-credits';

const activeStateTransitions = {
  'onHold': 'OnHold',
  'canceled': 'Canceled'
};

const onHoldStateTransitions = {
  'active': 'Active',
  'canceled': 'Canceled'
};

@connect((state, props) => ({
  ...state.customers.storeCredits[props.params.customerId]
}), StoreCreditsActions)
export default class StoreCredits extends React.Component {

  static propTypes = {
    params: PropTypes.object,
    tableColumns: PropTypes.array,
    fetchStoreCredits: PropTypes.func
  };

  static defaultProps = {
    tableColumns: [
      {
        field: 'createdAt',
        text: 'Date/Time Issued',
        type: 'date'
      },
      {
        field: 'storeCredit',
        text: 'Store Credit'
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

  componentDidMount() {
    const customerId = this.props.params.customerId;
    this.props.fetchStoreCredits(this.entityType(customerId));
  }

  entityType(customerId) {
    return {entityType: 'storeCredits', entityId: customerId};
  }

  @autobind
  renderRowState(rowId, rowState) {
    const customerId = this.props.params.customerId;
    switch(rowState) {
      case 'active':
        return (
          <Dropdown name="status"
                    items={ activeStateTransitions }
                    placeholder={ rowState }
                    value={ rowState }
                    onChange={ (value, title) =>
                      this.props.changeStatus(this.entityType(customerId), rowId, value) } />
        );
      case 'onHold':
        return (
          <Dropdown name="status"
                    items={ onHoldStateTransitions }
                    placeholder={ rowState }
                    value={ rowState }
                    onChange={ (value, title) =>
                      this.props.changeStatus(this.entityType(customerId), rowId, value) } />
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
        <TableCell>{ /* store credit, no data for it now */ }</TableCell>
        <TableCell>{ row.originType }</TableCell>
        <TableCell>{ /* store credit, no data for it too */ }</TableCell>
        <TableCell><Currency value={ row.originalBalance } /></TableCell>
        <TableCell><Currency value={ row.currentBalance } /></TableCell>
        <TableCell><Currency value={ row.availableBalance } /></TableCell>
        <TableCell>{ this.renderRowState(row.id, row.status) }</TableCell>
      </TableRow>
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
      </div>
    );
  }
}
