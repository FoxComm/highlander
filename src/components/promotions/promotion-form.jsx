
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from './promotion-form.css';

import ContentBox from '../content-box/content-box';
import ObjectForm from '../object-form/object-form';
import QualifierType from './qualifier-type';
import { Checkbox } from '../checkbox/checkbox';

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

  render() {
    const formAttributes = _.get(this.props, 'promotion.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'promotion.shadow.attributes', []);

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
            <QualifierType />
            <div styleName="sub-title">Qualifying Items</div>
            <Checkbox id="exclude-gc">Exclude gift cards</Checkbox>
          </ContentBox>
        </div>
        <div styleName="aside">
        </div>
      </div>
    );
  }
}
