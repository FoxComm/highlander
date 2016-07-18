
/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import { SelectableSearchList } from '../list-page';
import CouponRow from './coupon-row';
import { ChangeStateModal } from '../bulk-actions/modal';

import BulkWrapper from '../discounts/bulk';

// redux
import { actions } from '../../modules/coupons/list';

type CouponsProps = {
  actions: Object,
  list: Object,
};

const mapStateToProps = (state: Object) => {
  return {
    list: state.coupons.list,
  };
};

const mapDispatchToProps = (dispatch: Function) => {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
};

const tableColumns: Array<Object> = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'code', text: 'Code'},
  {field: 'totalUsed', text: 'Total Uses'},
  {field: 'currentCarts', text: 'Current Carts'},
  {field: 'createdAt', text: 'Date/Time Created', type: 'datetime'},
  {field: 'state', text: 'State'},
];

/* ::`*/
@connect(mapStateToProps, mapDispatchToProps)
/* ::`*/
export default class Coupons extends Component {
  props: CouponsProps;

  renderRow(row: Object, index: number, columns: Array<any>, params: Object): Element {
    const key = `coupon-${row.id}`;

    return (
      <CouponRow
        coupon={row}
        columns={columns}
        key={key}
        params={params}
      />
    );
  }

  render(): Element {
    const {list, actions} = this.props;

    return (
      <div className="coupons">
        <BulkWrapper entity="coupon">
          <SelectableSearchList
            emptyMessage="No coupons found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
          />
        </BulkWrapper>
      </div>
    );
  }
}
