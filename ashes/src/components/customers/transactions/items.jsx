
/* @flow */

/** Libs */
import { get, isString, capitalize } from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as dsl from '../../../elastic/dsl';

/** Components */
import ListPage from '../../list-page/list-page';
import CustomerItemsRow from './items-row';

/** Redux */
import { actions } from '../../../modules/customers/items';

type Customer = {
  id: number,
  name: string,
};

type Actions = {
  setExtraFilters: Function,
  fetch: Function,
};

type Props = {
  actions: Actions,
  list: Object,
  details: Object,
  customer: Customer,
};

const tableColumns = [
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'skuTitle', text: 'Name' },
  { field: 'skuCode', text: 'SKU' },
  { field: 'skuPrice', text: 'Price', type: 'currency' },
  { field: 'savedForLaterAt', text: 'Favorite' },
  { field: 'cordReferenceNumber', text: 'Order' },
  { field: 'orderPlacedAt', text: 'Date/Time Order Placed', type: 'datetime' },
];

/**
 * CustomerItems Component
 *
 * TODO: Add actions droprown when it's defined
 */
class CustomerItems extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.termFilter('customerId', this.props.customer.id)
    ]);

    this.props.actions.fetch();
  }

  renderRow(row: Object, index: number, columns: Array<any>, params: Object): Element<*> {
    const keyRow = `customer-items-${row.id}`;

    return <CustomerItemsRow item={row} columns={columns} params={params} key={keyRow}/>;
  }

  render() {
    const { props } = this;

    return (
      <div className="fc-customer-items">
        <ListPage
          entity="customers.transactions"
          addTitle="Items"
          documentTitle={`${props.customer.name || 'Customer'} Items`}
          emptyMessage="No purchased items found."
          list={props.list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchActions={props.actions}
          searchOptions={{singleSearch: true}}
          hasActionsColumn={false}
          title="Items"
        />
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
