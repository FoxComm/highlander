import _ from 'lodash';
import React, {Component} from 'react';
import { autobind } from 'core-decorators';
import { Dropdown } from '../../dropdown';

import styles from './discounts.css';
import { Checkbox } from '../../checkbox/checkbox';
import { FormField } from '../../forms';

import { OFFERS, QUALIFIERS } from './data';

const DISCOUNT_TYPES = QUALIFIERS.map( item => [item.discountType,item.text]);

const OFFER_TYPES = OFFERS.map(item => [item.type,item.text]);

const QUALIFIER_TYPES = QUALIFIERS.map( item => {
  const cell = {
    scope: item.discountType,
    list: item.qualifierTypes.map(i => [i.type,i.text]),
  };
  return cell;
});

export default class Discounts extends Component {
  qualifier = {};
  offer = {};

  constructor(props) {
    super(props);
    const discounts = this.props.discounts;
    this.qualifier = {
      ...discounts.qualifier,
    };
    this.offer = {
      ...discounts.offer,
    };
  }

  componentWillReceiveProps(props) {
    const discounts = props.discounts;
    this.qualifier = {
      ...discounts.qualifier,
    };
    this.offer = {
      ...discounts.offer,
    };
  }

  @autobind
  offerTypeChange(value) {
    const widget = _.find(OFFERS, i => i.type == value);
    this.offer = {
      ...this.offer,
      offerType: value,
      widgetValue: widget.value,
    };
    this.props.onChangeOffer(this.offer);
  }

  @autobind
  discountTypeChange(value) {
    const items = _.find(QUALIFIER_TYPES, i => i.scope == value).list;
    const qualifierType = _.get(items, '0.0');
    const qualifierTypes = _.find(QUALIFIERS, i => i.discountType == value).qualifierTypes;
    const widget = _.find(qualifierTypes, i => i.type == qualifierType);

    this.qualifier = {
      ...this.qualifier,
      discountType: value,
      qualifierType: qualifierType,
      widgetValue: widget.value,
      queryObject: widget.queryObject,
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  qualifierTypeChange(value) {
    const discountType = this.qualifier.discountType;
    const qualifierType = value;
    const qualifierTypes = _.find(QUALIFIERS, i => i.discountType == discountType).qualifierTypes;
    const widget = _.find(qualifierTypes, i => i.type == qualifierType);

    this.qualifier = {
      ...this.qualifier,
      qualifierType: value,
      widgetValue: widget.value,
      queryObject: widget.queryObject,
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  toggleExGiftCardQual() {
    this.qualifier = {
      ...this.qualifier,
      exGiftCardQual: !this.qualifier.exGiftCardQual,
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  toggleExGiftCardOffer() {
    this.offer = {
      ...this.offer,
      exGiftCardOffer: !this.offer.exGiftCardOffer,
    };
    this.props.onChangeOffer(this.offer);
  }

  @autobind
  setValueQual(value) {
    this.qualifier = {
      ...this.qualifier,
      widgetValue: value,
    };
    this.props.onChangeQualifier(this.qualifier);
  }


  @autobind
  setValueOffer(value) {
    this.offer = {
      ...this.offer,
      widgetValue: value,
    };
    this.props.onChangeOffer(this.offer);
  }

  @autobind
  setOfferShipingValue(value) {
    const widgetValue = {
      method: this.offer.widgetValue.method,
      value: value,
    };
    this.setValueOffer(widgetValue);
  }

  @autobind
  setOfferShipingMethod(value) {
    const widgetValue = {
      method: value,
      value: this.offer.widgetValue.value,
    };
    this.setValueOffer(widgetValue);
  }

  @autobind
  setQualQueryCond(value) {
    this.qualifier.queryObject = {
      ...this.qualifier.queryObject,
      conditions: value,
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  setQualQueryMain(value) {
    this.qualifier.queryObject = {
      ...this.qualifier.queryObject,
      mainCondition: value,
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  renderDiscount() {
    return(<Dropdown
      className="autowidth_dd"
      items={DISCOUNT_TYPES}
      value={this.qualifier.discountType}
      onChange={this.discountTypeChange}/>);
  }

  @autobind
  renderQualifier() {
    const discountType = this.qualifier.discountType;
    const items = _.find(QUALIFIER_TYPES, i => i.scope == discountType).list;
    return(<Dropdown
      className="autowidth_dd"
      items={items}
      value={this.qualifier.qualifierType}
      onChange={this.qualifierTypeChange}/>);
  }

  @autobind
  renderOffer() {
    return(<Dropdown
      className="autowidth_dd"
      items={OFFER_TYPES}
      value={this.offer.offerType}
      onChange={this.offerTypeChange}/>);
  }

  @autobind
  renderOfferWidget() {
    const comp = this;
    const offerType = this.offer.offerType;
    const renderWidget = _.find(OFFERS, i => i.type == offerType).template || function(){return null;};
    return renderWidget(comp);
  }


  @autobind
  renderQualWidget() {
    const comp = this;
    const discountType = this.qualifier.discountType;
    const qualifierType = this.qualifier.qualifierType;
    const qualifierTypes = _.find(QUALIFIERS, i => i.discountType == discountType).qualifierTypes;
    const renderWidget = _.find(qualifierTypes, i => i.type == qualifierType).template || function(){return null;};
    return renderWidget(comp);
  }

  @autobind
  renderOfferQueryBuilder() {
    const comp = this;
    const offerType = this.offer.offerType;
    const renderWidget = _.find(OFFERS, i => i.type == offerType).additional || function(){return null;};
    return renderWidget(comp);
  }

  @autobind
  renderQualifierQueryBuilder() {
    const comp = this;
    const discountType = this.qualifier.discountType;
    const qualifierType = this.qualifier.qualifierType;
    const qualifierTypes = _.find(QUALIFIERS, i => i.discountType == discountType).qualifierTypes;
    const renderWidget = _.find(qualifierTypes, i => i.type == qualifierType).additional || function(){return null;};
    return renderWidget(comp);
  }

  render(){
    return(
      <div styleName="discount_qualifier">
        <div styleName="sub-title">Qualifier</div>
        <FormField
          className="fc-object-form__field">
          <Checkbox id="isExGiftCardQual"
            inline
            checked={this.qualifier.exGiftCardQual}
            onChange={this.toggleExGiftCardQual}>
            <label htmlFor="isExGiftCardQual">Exclude gift cards from quaifying criteria</label>
          </Checkbox>
        </FormField>
        {this.renderDiscount()}
        {this.renderQualifier()}
        <div className="inline-container">{this.renderQualWidget()}</div>
        {this.renderQualifierQueryBuilder()}
        <div styleName="sub-title">Offer</div>
        <FormField
          className="fc-object-form__field">
          <Checkbox id="isExGiftCardOffer"
            inline
            checked={this.offer.exGiftCardOffer}
            onChange={this.toggleExGiftCardOffer}>
            <label htmlFor="isExGiftCardOffer">Exclude gift cards from discounted items</label>
          </Checkbox>
        </FormField>
        {this.renderOffer()}
        <div className="inline-container">{this.renderOfferWidget()}</div>
        {this.renderOfferQueryBuilder()}
      </div>
    );
  }
}