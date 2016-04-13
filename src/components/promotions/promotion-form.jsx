
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from './promotion-form.css';

import ContentBox from '../content-box/content-box';
import ObjectFormInner from '../object-form/object-form-inner';
import QualifierType from './qualifier-type';
import OfferType from './offer-type';
import { Checkbox } from '../checkbox/checkbox';
import { Dropdown, DropdownItem } from '../dropdown';
import ObjectScheduler from '../object-scheduler/object-scheduler';

import { setDiscountAttr } from '../../paragons/promotion';

export default class PromotionForm extends Component {

  get generalAttrs() {
    const toOmit = [
      'qualifier',
      'activeFrom',
      'activeTo',
    ];
    const shadow = _.get(this.props, 'promotion.shadow.attributes', []);
    return _(shadow).omit(toOmit).keys().value();
  }

  @autobind
  handleChange(form, shadow) {
    const newPromotion = assoc(this.props.promotion,
      ['form', 'attributes'], form,
      ['shadow', 'attributes'], shadow
    );

    this.props.onUpdatePromotion(newPromotion);
  }

  @autobind
  handleQualifierChange(qualifier) {
    const newPromotion = setDiscountAttr(this.props.promotion,
      'qualifier', 'qualifier', qualifier
    );

    this.props.onUpdatePromotion(newPromotion);
  }

  @autobind
  handleOfferChange(offer) {
    const newPromotion = setDiscountAttr(this.props.promotion,
      'offer', 'offer', offer
    );

    this.props.onUpdatePromotion(newPromotion);
  }

  @autobind
  handleApplyTypeChange(value) {
    const newPromotion = assoc(this.props.promotion, 'applyType', value);

    this.props.onUpdatePromotion(newPromotion);
  }

  get promotionState() {
    const { promotion } = this.props;
    const formAttributes = _.get(promotion, 'form.attributes', []);
    const shadowAttributes = _.get(promotion, 'shadow.attributes', []);
    const { applyType } = promotion;

    if (applyType == 'coupon') {
      return null;
    }

    return (
      <ObjectScheduler
        form={formAttributes}
        shadow={shadowAttributes}
        onChange={this.handleChange}
        title="Promotion" />
    );
  }

  render() {
    const { promotion } = this.props;
    const formAttributes = _.get(promotion, 'form.attributes', []);
    const shadowAttributes = _.get(promotion, 'shadow.attributes', []);

    const discount = {
      form: _.get(promotion, 'form.discounts.0', {}),
      shadow: _.get(promotion, 'shadow.discounts.0', {}),
    };

    return (
      <div styleName="promotion-form">
        <div styleName="main">
          <ContentBox title="General">
            <div className="fc-object-form__field">
              <div className="fc-object-form__field-label">Apply Type</div>
              <Dropdown
                placeholder="- Select -"
                value={promotion.applyType}
                onChange={this.handleApplyTypeChange}
              >
                <DropdownItem value="auto">Auto</DropdownItem>
                <DropdownItem value="coupon">Coupon</DropdownItem>
              </Dropdown>
            </div>
            <ObjectFormInner
              onChange={this.handleChange}
              fieldsToRender={this.generalAttrs}
              form={formAttributes}
              shadow={shadowAttributes}
            />
          </ContentBox>
          <ContentBox title="Qualifier">
            <div styleName="sub-title">Qualifier Type</div>
            <QualifierType discount={discount} onChange={this.handleQualifierChange} />
          </ContentBox>
          <ContentBox title="Offer">
            <div styleName="sub-title">Offer Type</div>
            <OfferType discount={discount} onChange={this.handleOfferChange} />
          </ContentBox>
        </div>
        <div styleName="aside">
          {this.promotionState}
        </div>
      </div>
    );
  }
}
