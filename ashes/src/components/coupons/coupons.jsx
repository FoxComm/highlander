
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
import { actions } from 'modules/coupons/list';

// helpers
import { filterArchived } from 'elastic/archive';

type CouponsProps = {
  actions: Object,
  list: Object,
  promotionId: Number,
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
  {field: 'codes', text: 'Codes'},
  {field: 'createdAt', text: 'Date/Time Created', type: 'datetime'},
  {field: 'totalUsed', text: 'Total Uses'},
  {field: 'usesPerCode', text: 'Max Uses'},
  {field: 'usesPerCustomer', text: 'Max Uses per Customer'},
  {field: 'currentCarts', text: 'Current Carts'},
];

/* ::`*/
@connect(mapStateToProps, mapDispatchToProps)
/* ::`*/
export default class Coupons extends Component {
  props: CouponsProps;

  @autobind
  applyPromotionFilter(filters: Array<SearchFilter>) {
    return [
      {
        term: 'promotionId',
        hidden: true,
        operator: 'eq',
        value: {
          type: 'identifier',
          value: String(this.props.promotionId)
        }
      },
      ...filters
    ];
  }

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    if(typeof this.props.promotionId === 'undefined'){
      return this.props.actions.addSearchFilters(filterArchived(filters), initial);
    }
    return this.props.actions.addSearchFilters(this.applyPromotionFilter(filterArchived(filters)), initial);
  }

  @autobind
  renderRow(row: Object, index: number, columns: Array<any>, params: Object): Element<*> {
    const key = `coupon-${row.id}`;
    return (
      <CouponRow
        coupon={row}
        columns={columns}
        key={key}
        params={params}
        promotionId={this.props.promotionId}
      />
    );
  }

  render(): Element<*> {
    const {list, actions} = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div className="coupons">
        <BulkWrapper entity="coupon">
          <SelectableSearchList
            entity="coupons.list"
            emptyMessage="No coupons found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={searchActions}
            autoRefresh={true}
          />
        </BulkWrapper>
      </div>
    );
  }
}
