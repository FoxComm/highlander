
/* @flow weak */

import _ from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from './promotion-form.css';

import ContentBox from '../content-box/content-box';
import FullObjectForm from '../object-form/full-object-form';
import { Dropdown, DropdownItem } from '../dropdown';
import FullObjectScheduler from '../object-scheduler/full-object-scheduler';
import { FormField, Form } from '../forms';
import SelectCustomerGroups from '../customers-groups/select-groups';
import FullObjectTags from '../tags/full-object-tags';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';
import Watchers from '../watchers/watchers';

import { setDiscountAttr } from '../../paragons/promotion';

type Props = {
  promotion: Object,
  onUpdatePromotion: Function,
};

type State = {
  qualifiedCustomerGroupIds: Array<any>,
};

export default class PromotionForm extends Component {
  props: Props;

  static propTypes = {
    promotion: PropTypes.object.isRequired,
    onUpdatePromotion: PropTypes.func.isRequired,
  };

  state: State = {
    qualifiedCustomerGroupIds: [], // it's temporary state until qualified customer groups not implented in backend!
  };

  get generalAttrs(): Array<any> {
    return ['name', 'storefrontName', 'description', 'details'];
  }

  @autobind
  handleChange(form: Object, shadow: Object) {
    const newPromotion = assoc(this.props.promotion,
      ['form', 'attributes'], form,
      ['shadow', 'attributes'], shadow
    );

    this.props.onUpdatePromotion(newPromotion);
  }

  @autobind
  handleQualifierChange(qualifier: Object) {
    const newPromotion = setDiscountAttr(this.props.promotion,
      'qualifier', 'qualifier', qualifier
    );

    this.props.onUpdatePromotion(newPromotion);
  }

  @autobind
  handleOfferChange(offer: Object) {
    const newPromotion = setDiscountAttr(this.props.promotion,
      'offer', 'offer', offer
    );

    this.props.onUpdatePromotion(newPromotion);
  }

  @autobind
  handleApplyTypeChange(value: any) {
    const newPromotion = assoc(this.props.promotion, 'applyType', value);

    this.props.onUpdatePromotion(newPromotion);
    this.refs.applyTypeField.autoValidate();
  }

  get promotionState(): ?Element {
    const { promotion } = this.props;
    const formAttributes = _.get(promotion, 'form.attributes', []);
    const shadowAttributes = _.get(promotion, 'shadow.attributes', []);
    const { applyType } = promotion;

    if (applyType == 'coupon') {
      return null;
    }

    return (
      <FullObjectScheduler
        parent="Promotions"
        form={formAttributes}
        shadow={shadowAttributes}
        onChange={this.handleChange}
        title="Promotion" />
    );
  }

  get watchersBlock(): ?Element {
    const { promotion } = this.props;

    if (promotion.form.id) {
      return <Watchers entity={{entityId: promotion.form.id, entityType: 'promotions'}} />;
    }
  }

  checkValidity(): bool {
    return this.refs.form.checkValidity();
  }

  render(): Element {
    const { promotion } = this.props;
    const formAttributes = _.get(promotion, 'form.attributes', []);
    const shadowAttributes = _.get(promotion, 'shadow.attributes', []);

    const discount = {
      form: _.get(promotion, 'form.discounts.0', {}),
      shadow: _.get(promotion, 'shadow.discounts.0', {}),
    };

    return (
      <Form ref="form" styleName="promotion-form">
        <div styleName="main">
          <ContentBox title="General">
            <FormField
              ref="applyTypeField"
              className="fc-object-form__field"
              label="Apply Type"
              getTargetValue={() => promotion.applyType}
              required
            >
              <div>
                <Dropdown
                  placeholder="- Select -"
                  value={promotion.applyType}
                  onChange={this.handleApplyTypeChange}
                >
                  <DropdownItem value="auto">Auto</DropdownItem>
                  <DropdownItem value="coupon">Coupon</DropdownItem>
                </Dropdown>
              </div>
            </FormField>
            <FullObjectForm
              onChange={this.handleChange}
              fieldsToRender={this.generalAttrs}
              form={formAttributes}
              shadow={shadowAttributes}
            />
          </ContentBox>
          <ContentBox title="Qualifier">
            <div styleName="sub-title">Qualifier Type</div>
            <DiscountAttrs
              discount={discount}
              attr="qualifier"
              descriptions={qualifiers}
              onChange={this.handleQualifierChange}
            />
          </ContentBox>
          <ContentBox title="Offer">
            <div styleName="sub-title">Offer Type</div>
            <DiscountAttrs
              discount={discount}
              attr="offer"
              descriptions={offers}
              onChange={this.handleOfferChange}
            />
          </ContentBox>
          <ContentBox title="Customers">
            <SelectCustomerGroups
              parent="Promotions"
              selectedGroupIds={this.state.qualifiedCustomerGroupIds}
              onSelect={(ids) => {
                this.setState({
                  qualifiedCustomerGroupIds: ids,
                });
              }}
            />
          </ContentBox>
        </div>
        <div styleName="aside">
          <FullObjectTags
            parent="Promotions"
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleChange}
          />
          {this.promotionState}
          {this.watchersBlock}
        </div>
      </Form>
    );
  }
}
