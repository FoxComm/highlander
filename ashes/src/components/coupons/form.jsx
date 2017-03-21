
/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import { searchCouponPromotions } from 'elastic/promotions';

// components
import DropdownSearch from '../dropdown/dropdown-search';
import DropdownItem from '../dropdown/dropdownItem';
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
        createCoupon={this.props.createCoupon}
        promotionId={id}
        codeGeneration={this.props.codeGeneration}
        isNew={this.props.isNew}
      />
    );
  }

  renderUsageRules() {
    return (
      <UsageRules {...(this.usageRules)} onChange={this.handleUsageRulesChange} />
    );
  }
};


