
/* @flow weak */

import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import classNames from 'classnames';

import styles from '../object-page/object-details.css';

import ObjectDetails from '../object-page/object-details';
import { FormField } from '../forms';
import RadioButton from '../forms/radio-button';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';

import ContentBox from 'components/content-box/content-box';
import { Button } from 'components/core/button';

import { setDiscountAttr } from 'paragons/promotion';
import { setObjectAttr, omitObjectAttr } from 'paragons/object';
import { customerGroups } from 'paragons/object-types';
const layout = require('./layout.json');

export default class ContentTypeForm extends ObjectDetails {

  layout = layout;

  renderApplyType() {
    const promotion = this.props.object;
    return (
      <FormField
        ref="applyTypeField"
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="autoApplyRadio"
            onChange={this.handleApplyTypeChange}
            name="auto"
            checked={promotion.applyType === 'auto'}>
            <label htmlFor="autoApplyRadio" styleName="field-label">Promotion is automatically applied</label>
          </RadioButton>
          <RadioButton id="couponCodeRadio"
            onChange={this.handleApplyTypeChange}
            name="coupon"
            checked={promotion.applyType === 'coupon'}>
            <label htmlFor="couponCodeRadio" styleName="field-label">Promotion requires a coupon code</label>
          </RadioButton>
        </div>
      </FormField>
    );
  }


  get usageRules() {
    return _.get(this.props, 'object.attributes.usageRules.v', {});
  }

  renderUsageRules() {
    const isExclusive = _.get(this.usageRules, 'isExclusive');
    return (
      <FormField
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="isExlusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="true"
            checked={isExclusive === true}>
            <label htmlFor="isExlusiveRadio">Promotion is exclusive</label>
          </RadioButton>
          <RadioButton id="notExclusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="false"
            checked={isExclusive === false}>
            <label htmlFor="notExclusiveRadio">Promotion can be used with other promotions</label>
          </RadioButton>
        </div>
      </FormField>
    );
  }

  @autobind
  handleQualifierChange(qualifier: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'qualifier', qualifier
    );

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleOfferChange(offer: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'offer', offer
    );

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleApplyTypeChange({target}: Object) {
    const value = target.getAttribute('name');
    const newPromotion = assoc(this.props.object, 'applyType', value);

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleUsageRulesChange({target}: Object) {
    const value = (target.getAttribute('name') === 'true');
    const newPromotion = setObjectAttr(this.props.object, 'usageRules', {
      t: 'PromoUsageRules',
      v: {
        'isExclusive': value
      }
    });

    this.props.onUpdateObject(newPromotion);
  }

  renderState(): ?Element<*> {
    return super.renderState();
  }

  renderDiscounts() {
    let discountChilds = [];
    const discounts = _.get(this.props.object, 'discounts', []);
    discounts.map((disc,index) => {
      discountChilds.push(<div styleName="sub-title">Qualifier</div>),
      discountChilds.push(<DiscountAttrs
        blockId={'promo-qualifier-block-'+index}
        dropdownId={'promo-qualifier-dd-'+index}
        discount={disc}
        attr="qualifier"
        descriptions={qualifiers}
        onChange={this.handleQualifierChange}
      />);
      discountChilds.push(<div styleName="sub-title">Offer</div>),
      discountChilds.push(<DiscountAttrs
        blockId={'promo-offer-block-'+index}
        dropdownId={'promo-offer-dd-'+index}
        discount={disc}
        attr="offer"
        descriptions={offers}
        onChange={this.handleOfferChange}
      />);
    });
    return (
      <div>
        {discountChilds}
      </div>
    );
  }

  @autobind
  handleQualifyAllChange(isAllQualify) {
    const promotion = this.props.object;
    let newPromotion;
    if (isAllQualify) {
      newPromotion = omitObjectAttr(promotion, 'customerGroupIds');
    } else {
      newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups([]));
    }
    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleQualifierGroupChange(ids){
    const promotion = this.props.object;
    const newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups(ids));
    this.props.onUpdateObject(newPromotion);
  }

  column(title: string, children): Element<*> {
    const footer = (
      <div styleName="column-footer">
        <Button
          icon="add"
          onClick={(e)=>{e.preventDefault();alert('ypa');}}
        >
          {title}
        </Button>
      </div>
    );

    const isEmpty = !children;

    const emptyBody = !isEmpty ? null : (
      <span>
        {`Add a ${title.toLowerCase()}`}
      </span>
    );

    const bodyClassName = classNames(
      styles['column-body'],
      {[styles['column-body-empty']]: isEmpty}
    );

    return (
      <ContentBox
        className={styles['column']}
        bodyClassName={bodyClassName}
        title={title}
        actionBlock={this.actions}
        footer={footer}
        indentContent={false}
      >
        {children}
        {emptyBody}
      </ContentBox>
    );
  }

  renderColumns(): Element<*> {
    const details = (
      <Button
        onClick={(e)=>{e.preventDefault();alert('ypa');}}
      >
        Details
      </Button>
    );

    return (
      <div styleName="columns">
        {this.column('Tab', details)}
        {this.column('Section')}
        {this.column('Properties')}
        {this.column('Property Settings')}
      </div>
    );
  }
}
