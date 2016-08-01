
/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import { searchCouponPromotions } from '../../elastic/promotions';

// components
import FullObjectForm from '../object-form/full-object-form';
import ContentBox from '../content-box/content-box';
import DropdownSearch from '../dropdown/dropdown-search';
import DropdownItem from '../dropdown/dropdownItem';
import RadioButton from '../forms/radio-button';
import CouponCodes from './form/coupon-codes';
import UsageRules from './form/usage-rules';
import { FormField, Form } from '../forms';
import FullObjectTags from '../tags/full-object-tags';
import FullObjectScheduler from '../object-scheduler/full-object-scheduler';
import Watchers from '../watchers/watchers';

// paragon
import { options } from '../../paragons/coupons';

// styles
import styles from './form.css';

type Entity = {
  entityId: number|string,
};

type CouponFormProps = {
  promotionError: bool,
  coupon: Object,
  onUpdateCoupon: Function,
  onGenerateBulkCodes: Function,
  onUpdateCouponCode: Function,
  fetchPromotions: Function,
  entity: Entity,
  saveCoupon: Function,
};

export default class CouponForm extends Component {

  static props: CouponFormProps;

  get generalAttrs() {
    return ['name', 'storefrontName', 'description', 'details'];
  }

  handlePromotionSearch(token: string) {
    return searchCouponPromotions(token).then((result) => {
      return result.result;
    });
  }

  @autobind
  renderPromotionOption(promotion) {
    return (
      <DropdownItem value={promotion.id} key={`${promotion.id}-${promotion.promotionName}`}>
        <span>{ promotion.promotionName }</span>
        <span styleName="text-gray">
          &nbsp;<span className="fc-icon icon-dot"></span>&nbsp;ID: { promotion.id }
        </span>
      </DropdownItem>
    );
  }

  get promotionSelector() {
    const id = _.get(this.props, 'coupon.promotion');
    return (
      <div>
        <div styleName="field-label">
          <label htmlFor="promotionSelector">
            Promotion
          </label>
        </div>
        <div>
          <DropdownSearch
            id="promotionSelector"
            styleName="full-width"
            name="promotion"
            placeholder="- Select -"
            value={id}
            onChange={(value) => this.handlePromotionChange(value)}
            fetchOptions={this.handlePromotionSearch}
            renderOption={this.renderPromotionOption}
            searchbarPlaceholder="Promotion name or storefront name"
          />
        </div>
        { this.props.promotionError && (<div className="fc-form-field-error">
          Promotion must be selected from the list above.
        </div>) }
        <div styleName="field-comment">
          Only promotions with the Coupon Apply Type can be attached to a coupon.
        </div>
      </div>
    );
  }

  @autobind
  handleChange(form, shadow) {
    const newCoupon = assoc(this.props.coupon,
      ['form', 'attributes'], form,
      ['shadow', 'attributes'], shadow
    );

    this.props.onUpdateCoupon(newCoupon);
  }

  @autobind
  handlePromotionChange(value) {
    const coupon = assoc(this.props.coupon, 'promotion', value);
    this.props.onUpdateCoupon(coupon);
  }

  @autobind
  handleUsageRulesChange(field, value) {
    const newCoupon = assoc(this.props.coupon, ['form', 'attributes', 'usageRules', field], value);
    this.props.onUpdateCoupon(newCoupon);
  }

  get isNew() {
    return this.props.entity.entityId === 'new';
  }

  get usageRules() {
    return _.get(this.props, 'coupon.form.attributes.usageRules', {});
  }

  get watchersBlock(): ?Element {
    const { coupon } = this.props;

    if (coupon.form.id) {
      return <Watchers entity={{entityId: coupon.form.id, entityType: 'coupons'}} />;
    }
  }

  checkValidity(): boolean {
    return this.refs.form.checkValidity();
  }

  render() {
    const formAttributes = _.get(this.props, 'coupon.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'coupon.shadow.attributes', []);

    return (
      <Form ref="form" styleName="coupon-form">
        <div styleName="main">
          <ContentBox title="General">
            <FullObjectForm
              onChange={this.handleChange}
              fieldsToRender={this.generalAttrs}
              form={formAttributes}
              shadow={shadowAttributes}
              options={options}
            />
            {this.promotionSelector}
          </ContentBox>
          <CouponCodes
            saveCoupon={this.props.saveCoupon}
            codeGeneration={this.props.codeGeneration}
            isNew={this.isNew}
          />
          <UsageRules {...(this.usageRules)} onChange={this.handleUsageRulesChange}/>
        </div>
        <div styleName="aside">
          <FullObjectTags
            parent="Coupons"
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleChange}
          />
          <FullObjectScheduler
            parent="Coupons"
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleChange}
            title="Coupon"
          />
          {this.watchersBlock}
        </div>
      </Form>
    );
  }

};


