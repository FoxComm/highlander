
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

// types
import type { SearchFilter } from 'elastic/common';

type CouponsProps = {
  actions: Object,
  list: Object,
  promoId: Number|undefined
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
  {field: 'maxUses', text: 'Max Uses'},
  {field: 'maxUsesPerCustomer', text: 'Max Uses per Customer'},
  {field: 'currentCarts', text: 'Current Carts'},
  {field: 'state', text: 'State'},
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
          value: String(this.props.promoId)
        }
      },
      ...filters
    ];
  }

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    if(typeof this.props.promoId === 'undefined'){
      return this.props.actions.addSearchFilters(filterArchived(filters), initial);
    }
    return this.props.actions.addSearchFilters(this.applyPromotionFilter(filterArchived(filters)), initial);
  }

  @autobind
  renderRow(row: Object, index: number, columns: Array<any>, params: Object, promoId: Number = this.props.promoId): Element {
    const key = `coupon-${row.id}`;
    return (
      <CouponRow
        coupon={row}
        columns={columns}
        key={key}
        params={params}
        promoId={promoId}
      />
    );
  }

  render(): Element {
    console.log(this.props);
    const {list, actions, promoId} = this.props;

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
          />
        </BulkWrapper>
      </div>
    );
  }
}
