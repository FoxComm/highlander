/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import { ListPageContainer, makeTotalCounter } from 'components/list-page';
import ShippingMethodRow from './row';
import MultiSelectTable from 'components/table/multi-select-table';
import SubNav from './sub-nav';

// actions
import * as shippingMethodActions from 'modules/shipping-methods/list';

// styles
import styles from './list.css';

// types
import type { ShippingMethod } from 'paragons/shipping-method';

type Props = {
  list: {
    shippingMethods: Array<ShippingMethod>,
  },
  isFetching: boolean,
  fetchError: ?Object,
  fetchShippingMethods: Function,
};

const mapStateToProps = (state) => {
  return {
    list: state.shippingMethods.list,
    isFetching: _.get(state.asyncActions, 'fetchShippingMethods.inProgress', null),
    fetchError: _.get(state.asyncActions, 'fetchShippingMethods.err', null),
  };
};

const tableColumns = [
  { field: 'id', text: 'ID' },
  { field: 'code', text: 'Code' },
  { field: 'adminDisplayName', text: 'Name' },
  { field: 'price', text: 'Price', type: 'currency' },
];

class ShippingMethodsList extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchShippingMethods();
  }

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `shipping-method-${row.id}`;

      return (
        <ShippingMethodRow
          key={key}
          shippingMethod={row}
          columns={columns}
          params={params} />
      );
    };
  }

  render(): Element {
    const { list, isFetching } = this.props;
    const count = list.shippingMethods.length;

    const props = isFetching || count == 0
      ? { title: 'Shipping Methods' }
      : { title: 'Shipping Methods', subtitle: count };

    return (
      <ListPageContainer {...props}>
        <SubNav />
        <div styleName="body">
          <div styleName="content">
            <MultiSelectTable
              columns={tableColumns}
              data={{ rows: list.shippingMethods }}
              isLoading={isFetching}
              emptyMessage="No shipping methods found."
              renderRow={this.renderRow} />
          </div>
        </div>
      </ListPageContainer>
    );
  }
}

export default connect(mapStateToProps, shippingMethodActions)(ShippingMethodsList);
