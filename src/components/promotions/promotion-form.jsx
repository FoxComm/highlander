
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from './promotion-form.css';

import ContentBox from '../content-box/content-box';
import ObjectForm from '../object-form/object-form';
import QualifierType from './qualifier-type';
import OfferType from './offer-type';
import { Checkbox } from '../checkbox/checkbox';

import { setDiscountAttr } from '../../paragons/promotion';

export default class PromotionForm extends Component {

  get generalAttrs() {
    const toOmit = [
      'qualifier'
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
          <ObjectForm
            onChange={this.handleChange}
            fieldsToRender={this.generalAttrs}
            form={formAttributes}
            shadow={shadowAttributes}
            title="General" />
          <ContentBox title="Qualifier">
            <div styleName="sub-title">Qualifier Type</div>
            <QualifierType discount={discount} onChange={this.handleQualifierChange} />
            <div styleName="sub-title">Qualifying Items</div>
            <Checkbox id="exclude-gc">Exclude gift cards</Checkbox>
          </ContentBox>
          <ContentBox title="Offer">
            <div styleName="sub-title">Offer Type</div>
            <OfferType discount={discount} onChange={this.handleOfferChange} />
          </ContentBox>
        </div>
        <div styleName="aside">
        </div>
      </div>
    );
  }
}
