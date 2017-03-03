import _ from 'lodash';
import React, {Component} from 'react';
import { autobind } from 'core-decorators';
import { Dropdown } from '../../dropdown';

import Currency from './currency';
import Counter from './counter';
import Percent from './percent';
import styles from './discounts.css';
import {Checkbox} from '../../checkbox/checkbox';
import { FormField } from '../../forms';

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
						<div>Order <Counter onChange={comp.setValue} value={comp.qualifier.widgetValue}/> or more</div>
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
						<div>Spend <Currency onChange={comp.setValue} value={comp.qualifier.widgetValue}/> or more</div>
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
						<div>Order <Counter onChange={comp.setValue} value={comp.qualifier.widgetValue}/> or more of the following items</div>
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
						<div>Spend <Currency onChange={comp.setValue} value={comp.qualifier.widgetValue}/> or more on following items</div>
					);	
				}
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

const OFFER_TYPES = [
	['orderPercentOff','Percent off order'],
	['orderAmountOff','Amount off order'],
	['itemsPercentOff','Percent off items'],
	['itemsAmountOff','Amount off items'],
	['freeShipping','Free shiping'],
	['discountedShipping','Discounted shiping'],
	['giftWithPurchase','Gift with purchase'],
	['chooseGiftWithPurchase','Your choice of with purchase'],
];

export default class Discounts extends Component {
	qualifier = {};
	offer = {};

	constructor(props) {
		super(props);
		let discounts = this.props.discounts;
		this.qualifier = {		
			...discounts.qualifier,
		}; 
		this.offer = {		
			...discounts.offer
		}; 
	}

	componentWillReceiveProps(props) {
		let discounts = props.discounts;
		this.qualifier = {		
			...discounts.qualifier,
		}; 
		this.offer = {		
			...discounts.offer
		}; 
	}

	@autobind
	offerTypeChange(value) {
		this.offer = {
			...this.offer,
			offerType: value,
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
	setValue(value) {
		this.qualifier = {
			...this.qualifier,
			widgetValue: value
		};
		this.props.onChangeQualifier(this.qualifier);
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
				<Dropdown 
					className="autowidth_dd"
					items={OFFER_TYPES}
					value={this.offer.offerType}
					onChange={this.offerTypeChange}/>			
			</div>
		);
	}
}
