import React, { PropTypes } from 'react';
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';
import { connect } from 'react-redux';
import * as StoreCreditTransactionsActions from '../../../modules/customers/store-credit-transactions';

@connect((state, props) => ({
  ...state.customers.storeCreditTransactions[props.params.customerId]
}), StoreCreditTransactionsActions)
export default class StoreCreditTransactions extends React.Component {

  static propTypes = {
    params: PropTypes.object,
    tableColumns: PropTypes.array,
    fetchStoreCreditTransactions: PropTypes.func
  };

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

  componentDidMount() {
    const customerId = this.props.params.customerId;
    this.props.fetchStoreCreditTransactions({entityType: 'storeCreditTransactions', entityId: customerId});
  }

  renderRow(row) {
    return (
      <TableRow key={`storeCreditTransaction-row-${row.id}`}>
        <TableCell><DateTime value={ row.createdAt }/></TableCell>
        <TableCell>{ row.transaction }</TableCell>
        <TableCell>{ row.amount }</TableCell>
        <TableCell>{ row.paymentState }</TableCell>
        <TableCell><Currency value={ row.totalAvailableBalance } /></TableCell>
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
              columns={ props.tableColumns }
              data={ props.storeCreditTransactions }
              renderRow={ this.renderRow }
              setState={ props.setFetchParams } />
          </div>
        </div>
      </div>
    );
  }
}
