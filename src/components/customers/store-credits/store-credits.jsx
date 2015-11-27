import React, { PropTypes } from 'react';
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import SearchBar from '../../search-bar/search-bar';
import { connect } from 'react-redux';
import * as StoreCreditsActions from '../../../modules/customers/store-credits';

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

  renderRow(row) {
    render(
      <TableRow key={`storeCredits-row-${row.id}`}>
          <TableCell><DateTime value={ row.createdAt }/></TableCell>
        </TableRow>
    );
  }

  render() {
    const props = this.props;
    console.log(props);
    return (
      <div className="fc-store-credits fc-list-page">
        <Summary {...props} />
        <div className="fc-grid fc-list-page-content">
          <SearchBar />
          <div className="fc-col-md-1-1">
            <TableView
              columns={this.props.tableColumns}
              data={this.props.storeCredits}
              renderRow={this.renderRow}
              setState={this.props.setFetchParams}
              />
          </div>
        </div>
      </div>
    );
  }
}
