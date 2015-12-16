import _ from 'lodash';
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
import Status from '../../common/status';

@connect((state, props) => ({
  storeCreditTransactions: state.customers.storeCreditTransactions[props.params.customerId],
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
        field: 'debit',
        text: 'Amount',
        type: 'transaction'
      },
      {
        field: 'status',
        text: 'Payment State',
        type: 'status',
        model: 'storeCreditTransaction'
      },
      {
        field: 'availableBalance',
        text: 'Total Available Balance',
        type: 'currency'
      }
    ]
  };

  get customerId() {
    return this.props.params.customerId;
  }

  componentDidMount() {
    this.props.fetchStoreCreditTransactions(this.customerId);
  }

  renderRow(row) {
    return (
      <TableRow key={`storeCreditTransaction-row-${row.id}`}>
        <TableCell><DateTime value={row.createdAt}/></TableCell>
        <TableCell>{row.transaction}</TableCell>
        <TableCell><Currency value={-row.debit} isTransaction={true}/></TableCell>
        <TableCell><Status value={row.status} model={"storeCreditTransaction"}/></TableCell>
        <TableCell><Currency value={row.availableBalance} /></TableCell>
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
              data={props.storeCreditTransactions}
              renderRow={this.renderRow}
              setState={params => this.props.fetchStoreCreditTransactions(this.customerId, params)} />
          </div>
        </div>
      </div>
    );
  }
}
