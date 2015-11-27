import React, { PropTypes } from 'react';
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';

export default class StoreCreditTransactions extends React.Component {

  static defaultProps = {
    tableColumns: [
      {
        field: 'createdAt',
        text: 'Date/Time',
        type: 'date'
      },
      {
        field: 'transaction',
        text: 'Transaction'
      },
      {
        field: 'amount',
        text: 'Amount'
      },
      {
        field: 'paymentState',
        text: 'Payment State'
      },
      {
        field: 'totalAvailableBalance',
        text: 'Total Availabale Balance'
      }
    ]
  };

  renderRow(row) {

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
              data={props.StoreCreditTransactions}
              renderRow={this.renderRow}
              setState={props.setFetchParams}
              />
          </div>
        </div>
      </div>
    );
  }
}
