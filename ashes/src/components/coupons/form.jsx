
/* @flow weak */

// libs
import _ from 'lodash';
import React from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import CouponCodes from './form/coupon-codes';
import UsageRules from './form/usage-rules';
import ObjectDetails from '../object-page/object-details';

import type { DetailsProps } from '../object-page/object-details';

// styles
import styles from './form.css';

type CouponFormProps = {
  promotionError: boolean,
  codeGeneration: Object,
  onUpdateCoupon: Function,
  onGenerateBulkCodes: Function,
  onUpdateCouponCode: Function,
  fetchPromotions: Function,
  createCoupon: Function,
  refresh: Function,
  saveBulk: () => Promise<*>,
};

const layout = require('./layout.json');

export default class CouponForm extends ObjectDetails {
  props: CouponFormProps & DetailsProps;
  layout = layout;

  @autobind
  handleUsageRulesChange(field, value) {
    const newCoupon = assoc(this.props.object, ['attributes', 'usageRules', 'v', field], value);
    this.props.onUpdateObject(newCoupon);
  }

  get usageRules() {
    return _.get(this.props, 'object.attributes.usageRules.v', {});
  }

  renderCouponCodes() {
    const id = _.get(this.props, 'object.promotion', null);
    if (id == null) return null;
    return (
      <CouponCodes
        save={this.props.saveBulk}
        createCoupon={this.props.createCoupon}
        promotionId={id}
        codeGeneration={this.props.codeGeneration}
        refresh={this.props.refresh}
        isNew={this.props.isNew}
      />
    );
  }

  renderUsageRules() {
    return (
      <UsageRules {...(this.usageRules)} onChange={this.handleUsageRulesChange} />
    );
  }
}


