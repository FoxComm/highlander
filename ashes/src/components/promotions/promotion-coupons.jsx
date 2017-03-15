/* @flow */
import _ from 'lodash';
import React from 'react';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';
import styles from './promotion-coupons.css';

// components
import Coupons from '../coupons/coupons';
import { SectionTitle } from '../section-title';
import { PrimaryButton } from '../../components/common/buttons';

export default class PromoCouponsPage extends React.Component {
  constructor(props) {
    super(props);
  }

  @autobind
  addAction() {
    transitionTo('promotion-coupon-new', {promotionId: this.props.object.id});
  };

  render() {
  	const promotionId = this.props.object.id;
  	const children = this.props.children;
    return (
      <div styleName="promotion-coupons-page">
        <SectionTitle title="Coupons">
        	<PrimaryButton onClick={this.addAction} icon="add">Coupon</PrimaryButton>
        </SectionTitle>
        <Coupons promotionId={promotionId} />
        {children}
      </div>
    );
  }  
};