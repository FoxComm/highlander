import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';
import styles from './content-type-coupons.css';

// components
import Coupons from '../coupons/coupons';
import { PrimaryButton } from 'components/core/button';
import { SectionTitle } from '../section-title';

export default class PromoCouponsPage extends Component {
  @autobind
  addAction() {
    transitionTo('content-type-coupon-new', {promotionId: this.props.object.id});
  }

  render() {
    const promotionId = this.props.object.id;
    const children = this.props.children;
    const applyType = this.props.object.applyType;

    if (applyType == 'auto') return null;
    return (
      <div styleName="content-type-coupons-page">
        <SectionTitle title="Coupons">
          <PrimaryButton onClick={this.addAction} icon="add">Coupon</PrimaryButton>
        </SectionTitle>
        <Coupons promotionId={promotionId} />
        {children}
      </div>
    );
  }
}
