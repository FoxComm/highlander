import _ from 'lodash';
import React, {Component} from 'react';
import { autobind } from 'core-decorators';
import { Dropdown } from '../../dropdown';

import Currency from './currency';
import Counter from './counter';
import Percent from './percent';
import styles from './discounts.css';
import { Checkbox } from '../../checkbox/checkbox';
import { FormField } from '../../forms';

import ElasticQueryGenerator from 'components/query-builder/elastic-query-generator';


const QUALIFIERS = [
  {
    discountType: 'order',
    text: 'Order',
    qualifierTypes: [
      {
        type: 'noQualifier',
        text: 'No qualifier'
      },
      {
        type: 'numUnits',
        text: 'Total units in order',
        value: 0,
        widget: 'counter',
        template: (comp) => {
          return (
            <div>Order <Counter onChange={comp.setValueQual} value={comp.qualifier.widgetValue}/> or more</div>
          );
        }
      },
      {
        type: 'subTotal',
        text: 'Subtotal of order',
        value: 0,
        widget: 'currency',
        template: (comp) => {
          return (
            <div>Spend <Currency onChange={comp.setValueQual} value={comp.qualifier.widgetValue}/> or more</div>
          );
        }
      }
    ]
  },
  {
    discountType: 'item',
    text: 'Item',
    qualifierTypes: [
      {
        type: 'noQualifier',
        text: 'No qualifier'
      },
      {
        type: 'numUnits',
        text: 'Total units in order',
        value: 0,
        widget: 'counter',
        template: (comp) => {
          return (
            <div>
              Order <Counter onChange={comp.setValueQual}
                             value={comp.qualifier.widgetValue}/>
              or more of the following items
              </div>
          );
        },
        additional: (comp) => {
          return (
            <ElasticQueryGenerator/>
          );
        },
      },
      {
        type: 'subTotal',
        text: 'Subtotal of order',
        value: 0,
        widget: 'currency',
        template: (comp) => {
          return (
            <div>
              Spend <Currency onChange={comp.setValueQual}
                              value={comp.qualifier.widgetValue}/>
              or more on following items
            </div>
          );
        },
        additional: (comp) => {
          return (
            <ElasticQueryGenerator/>
          );
        },
      }
    ]
  }
];

const DISCOUNT_TYPES = QUALIFIERS.map(item => [item.discountType,item.text]);

const QUALIFIER_TYPES = QUALIFIERS.map( item => {
  let cell = {
    scope: item.discountType,
    list: item.qualifierTypes.map(i => [i.type,i.text])
  };
  return cell;
});

const OFFERS = [
  {
    type: 'orderPercentOff',
    text: 'Percent off order',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Percent onChange={comp.setValueOffer} value={comp.offer.widgetValue}/> off your order</div>
      );
    },
  },
  {
    type: 'orderAmountOff',
    text: 'Amount off order',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Currency onChange={comp.setValueOffer} value={comp.offer.widgetValue}/> off the following items</div>
      );
    },
  },
  {
    type: 'itemsPercentOff',
    text: 'Percent off items',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Percent onChange={comp.setValueOffer} value={comp.offer.widgetValue}/> off your order</div>
      );
    },
    additional: (comp) => {
      return (
        <ElasticQueryGenerator/>
      );
    },
  },
  {
    type: 'itemsAmountOff',
    text: 'Amount off items',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Currency onChange={comp.setValueOffer} value={comp.offer.widgetValue}/> off your order</div>
      );
    },
    additional: (comp) => {
      return (
        <ElasticQueryGenerator/>
      );
    },
  },
  {
    type: 'freeShipping',
    text: 'Free shiping',
    value: 'shiping1',
    template: (comp) => {
      return (
        <div>
          Get
          <Dropdown
            className="autowidth_dd"
            items={[['shiping1','shiping1'],['shiping2','shiping2']]}
            onChange={comp.setValueOffer}
            value={comp.offer.widgetValue}/>
          for free!
        </div>
      );
    },
  },
  {
    type: 'discountedShipping',
    text: 'Discounted shiping',
    value: {
      method: 'shiping1',
      value: 0,
    },
    template: (comp) => {
      return (
        <div>
          Get
          <Dropdown
            className="autowidth_dd"
            items={[['shiping1','shiping1'],['shiping2','shiping2']]}
            onChange={comp.setOfferShipingMethod}
            value={comp.offer.widgetValue.method}/>
          for
          <Currency
            onChange={comp.setOfferShipingValue}
            value={comp.offer.widgetValue.value}/>
        </div>
      );
    },
  },
  {
    type: 'giftWithPurchase',
    text: 'Gift with purchase',
    value: 0,
  },
  {
    type: 'chooseGiftWithPurchase',
    text: 'Your choice of with purchase',
    value: 0,
  },
];


const OFFER_TYPES = OFFERS.map(item => [item.type,item.text]);

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
      ...discounts.offer
    };
  }

  componentWillReceiveProps(props) {
    const discounts = props.discounts;
    this.qualifier = {
      ...discounts.qualifier,
    };
    this.offer = {
      ...discounts.offer
    };
  }

  @autobind
  offerTypeChange(value) {
    let widgetValue = _.find(OFFERS, i => i.type == value).value || null;
    this.offer = {
      ...this.offer,
      offerType: value,
      widgetValue: widgetValue
    };
    this.props.onChangeOffer(this.offer);
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
    let discountType = this.qualifier.discountType;
    let items = _.find(QUALIFIER_TYPES, i => i.scope == discountType).list;
    return(<Dropdown
      className="autowidth_dd"
      items={items}
      value={this.qualifier.qualifierType}
      onChange={this.qualifierTypeChange}/>);
  }

  @autobind
  discountTypeChange(value) {
    let items = _.find(QUALIFIER_TYPES, i => i.scope == value).list;
    let qualifierType = _.get(items, '0.0');
    let qualifierTypes = _.find(QUALIFIERS, i => i.discountType == value).qualifierTypes;
    let widgetValue = _.find(qualifierTypes, i => i.type == qualifierType).value || null;

    this.qualifier = {
      ...this.qualifier,
      discountType: value,
      qualifierType: qualifierType,
      widgetValue: widgetValue
    };
    this.props.onChangeQualifier(this.qualifier);
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
    let comp = this;
    let offerType = this.offer.offerType;
    let renderWidget = _.find(OFFERS, i => i.type == offerType).template || function(){return null;};
    return renderWidget(comp);
  }

  @autobind
  qualifierTypeChange(value) {
    let discountType = this.qualifier.discountType;
    let qualifierType = value;
    let qualifierTypes = _.find(QUALIFIERS, i => i.discountType == discountType).qualifierTypes;
    let widgetValue = _.find(qualifierTypes, i => i.type == qualifierType).value || null;

    this.qualifier = {
      ...this.qualifier,
      qualifierType: value,
      widgetValue: widgetValue
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  renderQualWidget() {
    let comp = this;
    let discountType = this.qualifier.discountType;
    let qualifierType = this.qualifier.qualifierType;
    let qualifierTypes = _.find(QUALIFIERS, i => i.discountType == discountType).qualifierTypes;
    let renderWidget = _.find(qualifierTypes, i => i.type == qualifierType).template || function(){return null;};
    return renderWidget(comp);
  }

  @autobind
  toggleExGiftCardQual() {
    this.qualifier = {
      ...this.qualifier,
      exGiftCardQual: !this.qualifier.exGiftCardQual
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  toggleExGiftCardOffer() {
    this.offer = {
      ...this.offer,
      exGiftCardOffer: !this.offer.exGiftCardOffer
    };
    this.props.onChangeOffer(this.offer);
  }

  @autobind
  setValueQual(value) {
    this.qualifier = {
      ...this.qualifier,
      widgetValue: value
    };
    this.props.onChangeQualifier(this.qualifier);
  }

  @autobind
  setValueOffer(value) {
    this.offer = {
      ...this.offer,
      widgetValue: value
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
  renderOfferQueryBuilder() {
    let comp = this;
    let offerType = this.offer.offerType;
    let renderWidget = _.find(OFFERS, i => i.type == offerType).additional || function(){return null;};
    return renderWidget(comp);
  }

  @autobind
  renderQualifierQueryBuilder() {
    let comp = this;
    let discountType = this.qualifier.discountType;
    let qualifierType = this.qualifier.qualifierType;
    let qualifierTypes = _.find(QUALIFIERS, i => i.discountType == discountType).qualifierTypes;
    let renderWidget = _.find(qualifierTypes, i => i.type == qualifierType).additional || function(){return null;};
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