import React from 'react';
import _ from 'lodash';

import Currency from './currency';
import Counter from './counter';
import Percent from './percent';
import QueryBuilderContainer from 'components/query-builder/query-builder-container';

import criterions, { getCriterion, getOperators, getWidget } from 'paragons/query-builder/promotions-criterions';

const OFFERS = [
  {
    type: 'orderPercentOff',
    text: 'Percent off order',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Percent
                    onChange={comp.setValueOffer}
                    value={comp.offer.widgetValue}/> off your order</div>
      );
    },
  },
  {
    type: 'orderAmountOff',
    text: 'Amount off order',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Currency
                    onChange={comp.setValueOffer}
                    value={comp.offer.widgetValue}/> off the following items</div>
      );
    },
  },
  {
    type: 'itemsPercentOff',
    text: 'Percent off items',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Percent
                    onChange={comp.setValueOffer}
                    value={comp.offer.widgetValue}/> off your order</div>
      );
    },
    additional: (comp) => {
      return (
        <QueryBuilderContainer
          criterions={criterions}
          getCriterion={getCriterion}
          getOperators={getOperators}
          getWidget={getWidget}
          mainCondition={_.get(comp, 'qualifier.queryObject.mainCondition', '$and')}
          conditions={_.get(comp, 'qualifier.queryObject.conditions', [])}
          setMainCondition={comp.setQualQueryMain}
          setConditions={comp.setQualQueryCond}/>
      );
    },
  },
  {
    type: 'itemsAmountOff',
    text: 'Amount off items',
    value: 0,
    template: (comp) => {
      return (
        <div>Get <Currency
                    onChange={comp.setValueOffer}
                    value={comp.offer.widgetValue}/> off your order</div>
      );
    },
    additional: (comp) => {
      return (
        <QueryBuilderContainer
          criterions={criterions}
          getCriterion={getCriterion}
          getOperators={getOperators}
          getWidget={getWidget}
          mainCondition={_.get(comp, 'qualifier.queryObject.mainCondition', '$and')}
          conditions={_.get(comp, 'qualifier.queryObject.conditions', [])}
          setMainCondition={comp.setQualQueryMain}
          setConditions={comp.setQualQueryCond}/>
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
          Get <Dropdown
                className="autowidth_dd"
                items={[['shiping1','shiping1'],['shiping2','shiping2']]}
                onChange={comp.setValueOffer}
                value={comp.offer.widgetValue}/> for free!
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
          Get <Dropdown
                className="autowidth_dd"
                items={[['shiping1','shiping1'],['shiping2','shiping2']]}
                onChange={comp.setOfferShipingMethod}
                value={comp.offer.widgetValue.method}
                /> for <Currency
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
    text: 'Your choice of gift with purchase',
    value: 0,
  },
];

const QUALIFIERS = [
  {
    discountType: 'order',
    text: 'Order',
    qualifierTypes: [
      {
        type: 'noQualifier',
        text: 'No qualifier',
      },
      {
        type: 'numUnits',
        text: 'Total units in order',
        value: 0,
        widget: 'counter',
        template: (comp) => {
          return (
            <div>Order <Counter
                          onChange={comp.setValueQual}
                          value={comp.qualifier.widgetValue}/> or more</div>
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
            <div>Spend <Currency
                          onChange={comp.setValueQual}
                          value={comp.qualifier.widgetValue}/> or more</div>
          );
        },
      },
    ],
  },
  {
    discountType: 'item',
    text: 'Item',
    qualifierTypes: [
      {
        type: 'noQualifier',
        text: 'No qualifier',
      },
      {
        type: 'numUnits',
        text: 'Total units in order',
        value: 0,
        widget: 'counter',
        queryObject: {
          mainCondition: '$and',
          conditions: [],
        },
        template: (comp) => {
          return (
            <div>
              Order <Counter
                      onChange={comp.setValueQual}
                      value={comp.qualifier.widgetValue}
                      /> or more of the following items
            </div>
          );
        },
        additional: (comp) => {
          return (
            <QueryBuilderContainer
              criterions={criterions}
              getCriterion={getCriterion}
              getOperators={getOperators}
              getWidget={getWidget}
              mainCondition={_.get(comp, 'qualifier.queryObject.mainCondition', '$and')}
              conditions={_.get(comp, 'qualifier.queryObject.conditions', [])}
              setMainCondition={comp.setQualQueryMain}
              setConditions={comp.setQualQueryCond}/>
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
              Spend <Currency
                      onChange={comp.setValueQual}
                      value={comp.qualifier.widgetValue}
                      /> or more on following items
            </div>
          );
        },
        additional: (comp) => {
          return (
            <QueryBuilderContainer
              criterions={criterions}
              getCriterion={getCriterion}
              getOperators={getOperators}
              getWidget={getWidget}
              mainCondition={_.get(comp, 'qualifier.queryObject.mainCondition', '$and')}
              conditions={_.get(comp, 'qualifier.queryObject.conditions', [])}
              setMainCondition={comp.setQualQueryMain}
              setConditions={comp.setQualQueryCond}/>
          );
        },
      },
    ],
  }
];

export {
  OFFERS,
  QUALIFIERS,
}