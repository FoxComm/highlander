
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

// components
import Summary from './summary';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import MultiSelectTable from '../../table/multi-select-table';
import { DateTime } from '../../common/datetime';
import Currency from '../../common/currency';
import SearchBar from '../../search-bar/search-bar';
import { Checkbox } from '../../checkbox/checkbox';
import State from '../../common/state';

// redux
import * as StoreCreditTransactionsActions from '../../../modules/customers/store-credit-transactions';

@connect((state, props) => ({
  storeCreditTransactions: state.customers.storeCreditTransactions[props.params.customerId],
}), StoreCreditTransactionsActions)
export default class StoreCreditTransactions extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  static propTypes = {
    params: PropTypes.object,
    tableColumns: PropTypes.array,
    fetchStoreCreditTransactions: PropTypes.func,
    fetchTotals: PropTypes.func
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
        type: 'state',
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
    this.props.fetchTotals(this.customerId);
  }

  renderRow(row) {
    return (
      <TableRow key={`storeCreditTransaction-row-${row.id}`}>
        <TableCell><Checkbox /></TableCell>
        <TableCell><DateTime value={row.createdAt}/></TableCell>
        <TableCell>{row.transaction}</TableCell>
        <TableCell><Currency value={-row.debit} isTransaction={true}/></TableCell>
        <TableCell><State value={row.status} model={"storeCreditTransaction"}/></TableCell>
        <TableCell><Currency value={row.availableBalance} /></TableCell>
      </TableRow>
    );
  }

  render() {
    const props = this.props;
    const totals = _.get(props, ['storeCreditTransactions', 'totals']);

    return (
      <div className="fc-store-credits fc-list-page">
        <Summary totals={totals}
                 params={props.params}
                 history={this.context.history}
                 transactionsSelected={true} />
        <div className="fc-grid fc-list-page-content">
          <SearchBar />
          <div className="fc-col-md-1-1">
            <MultiSelectTable
              columns={props.tableColumns}
              data={props.storeCreditTransactions}
              renderRow={this.renderRow}
              emptyMessage="No transactions found."
              toggleColumnPresent={false}
              setState={params => this.props.fetchStoreCreditTransactions(this.customerId, params)} />
          </div>
        </div>
      </div>
    );
  }
}
