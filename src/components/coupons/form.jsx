
/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import { searchCouponPromotions } from '../../elastic/promotions';

// components
import ObjectFormInner from '../object-form/object-form-inner';
import ContentBox from '../content-box/content-box';
import DropdownSearch from '../dropdown/dropdown-search';
import DropdownItem from '../dropdown/dropdownItem';
import RadioButton from '../forms/radio-button';
import CouponCodes from './form/coupon-codes';
import UsageRules from './form/usage-rules';
import { FormField, Form } from '../forms';
import Tags from '../tags/tags';
import ObjectScheduler from '../object-scheduler/object-scheduler';
import Watchers from '../watchers/watchers';

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
  handleGenerateBulkCodes(prefix, length, quantity) {
    this.props.onGenerateBulkCodes(prefix, length, quantity);
  }

  @autobind
  handleChangeSingleCode(value) {
    this.props.onUpdateCouponCode(value);
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

  render() {
    const formAttributes = _.get(this.props, 'coupon.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'coupon.shadow.attributes', []);

    return (
      <Form styleName="coupon-form">
        <div styleName="main">
          <ContentBox title="General">
            <ObjectFormInner
              onChange={this.handleChange}
              fieldsToRender={this.generalAttrs}
              form={formAttributes}
              shadow={shadowAttributes}
            />
            {this.promotionSelector}
          </ContentBox>
          <CouponCodes
            onGenerateBulkCodes={this.handleGenerateBulkCodes}
            onChangeSingleCode={this.handleChangeSingleCode}
          />
          <UsageRules {...(this.usageRules)} onChange={this.handleUsageRulesChange}/>
        </div>
        <div styleName="aside">
          <Tags
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleChange}
          />
          <ObjectScheduler
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleChange}
            title="State"
          />
          {this.watchersBlock}
        </div>
      </Form>
    );
  }

};


