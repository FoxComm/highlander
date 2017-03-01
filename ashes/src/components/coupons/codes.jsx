
/* @flow weak */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import CouponCodeRow from './coupon-code-row';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SectionSubtitle } from '../section-title';

// redux
import { actions as CouponCodesActions } from '../../modules/coupons/coupon-codes';
import { actions as ReduxBulkActions } from '../../modules/coupons/coupon-codes-bulk';

// styles
import styles from './form.css';

type CouponCodeParams = {
  couponId: string,
};

type CouponCodesActionType = {
  setExtraFilters: Function,
  fetch: Function,
};

type CouponCodesProps = {
  list: Array<any>,
  actions: CouponCodesActionType,
  params: CouponCodeParams,
};

const tableColumns = [
  { field: 'createdAt', text: 'Date/Time Created', type: 'date', },
  { field: 'code', text: 'Coupon Code', },
  { field: 'totalUsed', text: 'Total Uses', },
  { field: 'currentCarts', text: 'Current Carts', },
];

const mapStateToProps = (state, props) => ({
  list: state.coupons.couponCodes,
});

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(CouponCodesActions, dispatch),
    bulkActions: bindActionCreators(ReduxBulkActions, dispatch),
  };
};

/* ::`*/
@connect(mapStateToProps, mapDispatchToProps)
/* ::`*/
export default class CouponCodes extends Component {

  props: CouponCodesProps;

  get couponId(): string {
    return this.props.params.couponId;
  }

  componentDidMount(): void {
    this.props.actions.setExtraFilters([
      { term: { 'couponId': this.couponId } }
    ]);
    this.props.actions.fetch();
  }

  @autobind
  renderRow(row: Object, index: number, columns: Array<any>, params: Object): Element<*> {
    const key = `coupon-code-${row.code}`;
    return (
      <CouponCodeRow
        couponCode={row}
        columns={columns}
        key={key}
        params={params}
      />
    );
  }

  bulkActions(): Array<any> {
    return [];
  }

  renderDetail(): any {
    return null;
  }

  render() {
    const {list, actions} = this.props;

    const entity = 'couponCode';
    const module = `${entity}s`;

    return (
      <div className="fc-coupon-codes">
        <div styleName="subtitle">
          <SectionSubtitle>Coupon Codes</SectionSubtitle>
        </div>
        <BulkMessages
          storePath={`${module}.bulk`}
          module={module}
          entity={entity}
          renderDetail={this.renderDetail} />
        <BulkActions
          module={module}
          entity={entity}
          actions={this.bulkActions()} >
          <SelectableSearchList
            tbodyId="coupon-codes-list"
            entity="coupons.couponCodes"
            title="Coupon Codes"
            emptyMessage="No coupon codes found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={this.props.actions}
            searchOptions={{
              singleSearch: true,
              placeholder: 'keyword search',
            }}
          />
        </BulkActions>
      </div>
    );
  }
}


