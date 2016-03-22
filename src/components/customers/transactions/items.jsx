/** Libs */
import { get, isString, capitalize } from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as dsl from '../../../elastic/dsl';

/** Components */
import ListPage from '../../list-page/list-page';
import CustomerItemsRow from './items-row';

/** Redux */
import { actions } from '../../../modules/customers/items';

const tableColumns = [
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'skuTitle', text: 'Name' },
  { field: 'skuCode', text: 'SKU' },
  { field: 'skuPrice', text: 'Price', type: 'currency' },
  { field: 'savedForLaterAt', text: 'Favorite' },
  { field: 'orderReferenceNumber', text: 'Order' },
  { field: 'orderPlacedAt', text: 'Date/Time Order Placed', type: 'datetime' },
];


/**
 * CustomerItems Component
 * 
 * TODO: Add actions droprown when it's defined
 */
class CustomerItems extends Component {

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.termFilter('customerId', this.props.customer.id)
    ]);

    this.props.actions.fetch();
  }

  renderRow(row, index, columns, params) {
    const keyRow = `customer-items-${row.id}`;

    return <CustomerItemsRow item={row} columns={columns} params={params} key={keyRow}/>;
  }

  render() {
    return (
      <div className="fc-customer-items">
        <ListPage
          addTitle="Items"
          emptyResultMessage="No purchased items found."
          list={this.props.list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchActions={this.props.actions}
          searchOptions={{singleSearch: true}}
          toggleColumnPresent={false}
          title="Items"/>
      </div>
    );
  }
}

function mapState(state, props) {
  return {
    list: state.customers.items,
    customer: state.customers.details[props.params.customerId].details,
  };
}

function mapDispatch(dispatch) {
  return { actions: bindActionCreators(actions, dispatch) };
}

export default connect(mapState, mapDispatch)(CustomerItems);
