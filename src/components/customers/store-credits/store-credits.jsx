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
  'OnHold': 'OnHold',
  'Canceled': 'Canceled'
};

const onHoldStateTransitions = {
  'Active': 'Active',
  'Canceled': 'Canceled'
};

@connect((state, props) => ({
  ...state.customers.storeCredits[props.params.customerId]
}), StoreCreditsActions)
export default class StoreCredits extends React.Component {

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
    this.props.fetchStoreCredits({entityType: 'storeCredits', entityId: customerId});
  }

  @autobind
  renderRowState(rowState) {
    switch(rowState) {
      case 'active':
        return (
          <Dropdown name="status"
                    items={ activeStateTransitions }
                    placeholder={ rowState }
                    value={ rowState }
                    onChange={ () => { console.log('not implemented yet') } } />
        );
      case 'onHold':
        return (
          <Dropdown name="status"
                    items={ onHoldStateTransitions }
                    placeholder={ rowState }
                    value={ rowState }
                    onChange={ () => { console.log('not implemented yet') } } />
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
        <TableCell>{ this.renderRowState(row.status) }</TableCell>
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
