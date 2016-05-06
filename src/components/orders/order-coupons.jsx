
/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { configureCoupon } from '../../paragons/coupons';

import EditableContentBox from '../content-box/editable-content-box';
import ContentBox from '../content-box/content-box';
import PanelHeader from './panel-header';
import CouponRow from './discounts/coupon-row';
import TableView from '../table/tableview';
import { Button, EditButton } from '../common/buttons';
import AppendInput from '../forms/append-input';

import * as CouponActions from '../../modules/orders/coupons';

import styles from './styles/discounts.css';

import type { CouponModuleProps, CouponModuleActions } from '../../modules/orders/coupons';

type Target = {
  name: string,
  value: string|number|boolean,
};

type Props = CouponModuleActions & {
  readOnly: bool,
  isCart: bool,
  coupons: CouponModuleProps,
  order: {
    referenceNumber: string,
  }
};

const viewColumns = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'code', text: 'Code'},
];

const editColumns = viewColumns.concat([
  {field: 'edit', text: ''},
]);

const bindStateToProps = (state) => ({
  coupons: state.orders.coupons,
});

class OrderCoupons extends Component {
  props: Props;

  static defaultProps = {
    readOnly: false,
    isCart: false,
  };

  get isEditable(): bool {
    return this.props.readOnly || !this.props.isCart;
  }

  get isEditing(): bool {
    return this.props.coupons.isEditing;
  }

  get orderReferenceNumber(): string {
    return this.props.order.referenceNumber;
  }

  get title(): Element {
    return (
      <PanelHeader isCart={this.props.isCart} isOptional={true} text="Coupons" />
    );
  }

  get coupons(): Array<Object> {
    const coupon = _.get(this.props, 'order.coupon');

    if (!coupon) return [];

    return [coupon];
  }

  get viewContent(): Element {
    const coupons = this.coupons;
    if (_.isEmpty(coupons)) {
      return <div styleName="empty-message">No coupons applied.</div>;
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: coupons}}
          emptyMessage="No coupons applied."
          renderRow={this.renderRow(this.isEditing)}
        />
      );
    }
  }

  get editFooter(): Element {
    const plate = (
      <Button styleName="add-coupon-button" onClick={this.onAddClick}>Apply</Button>
    );
    const errorMessage = this.props.coupons.error && (
      <div className="fc-form-field-error">{this.fancyErrorMessage}</div>
    );
    return (
      <div styleName="add-coupon-block">
        <div styleName="add-coupon-label">
          <strong>Add Coupon</strong>
        </div>
        <AppendInput
          styleName="add-coupon-input-container"
          inputName="couponCode"
          value={this.props.coupons.code}
          inputClass={styles['add-coupon-input']}
          plate={plate}
          placeholder="Enter coupon code"
          onChange={this.orderCouponCodeChange}
        />
        {errorMessage}
      </div>
    );
  }

  get fancyErrorMessage(): ?string {
    const errorMessage = _.get(this.props, 'coupons.error');
    if (errorMessage && errorMessage.indexOf('not found') >= 0) {
      return 'This coupon code does not exist.';
    }
    if (errorMessage && errorMessage.indexOf('rejected') >= 0) {
      return 'Your order does not qualify for this coupon.';
    }
    if (errorMessage && errorMessage.indexOf('inactive') >= 0) {
      return 'This coupon code is inactive.';
    }
    return errorMessage;
  }

  @autobind
  onAddClick(): void {
    this.props.addCoupon(this.orderReferenceNumber);
  }

  @autobind
  orderCouponCodeChange(event: {target: Target}): void {
    this.props.orderCouponCodeChange(event.target.value);
  }

  renderRow(isEditing: bool): Function {
    const columns = isEditing ? editColumns : viewColumns;
    const renderFn = (row: Object, index: number, isNew: boolean) => {
      return (
        <CouponRow
          key={`order-coupon-row-${row.id}`}
          item={row}
          columns={columns}
          onDelete={() => this.props.removeCoupon(this.orderReferenceNumber)}
        />
      );
    };
    return renderFn;
  }

  render(): Element {
    const CouponsContentBox = this.isEditable
        ? ContentBox
        : EditableContentBox;

    return (
      <CouponsContentBox
        title={this.title}
        isTable={true}
        indentContent={false}
        viewContent={this.viewContent}
        editContent={this.viewContent}
        editFooter={this.editFooter}
        isEditing={this.isEditing}
        editAction={this.props.orderCouponsStartEdit}
        doneAction={this.props.orderCouponsStopEdit}
      />
    );
  }
};

export default connect(bindStateToProps, CouponActions)(OrderCoupons);
