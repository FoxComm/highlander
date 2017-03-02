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

const DISCOUNT_TYPES = [
	['order','Order'],
	['item','Item'],
];

const ITEM_QUALIFIER_TYPES = [
	['noQualifier','No qualifier'],
	['subtotalOfOrder','Subtotal of order'],
	['number','Total units in order'],
];

const ORDER_QUALIFIER_TYPES = [
	['noQualifier','No qualifier'],
	['subtotalOfOrder','Subtotal of order'],
	['number','Total units in order'],
];

export default class Discounts extends Component {
  constructor(props) {
    super(props);
    this.state = {		
	    	discountType: _.get(DISCOUNT_TYPES, '0.0'),
	    	offerType: _.get(OFFER_TYPES, '0.0'),
	    	qualifierType: _.get(ORDER_QUALIFIER_TYPES, '0.0'),
	    	orderAddon2: 0,
	    	orderAddon3: 1,
	    	itemAddon2: 0,
	    	itemAddon3: 1,
	    	exGiftCardQual: true,
	    	exGiftCardOffer: true,
	    	percent: 0,
			qualifierTypes: ORDER_QUALIFIER_TYPES
		};
  }

	@autobind
	discountTypeChange(value) {
		switch (value) {
			case 'order':
				this.setState({
					discountType: value,
		    		qualifierType: _.get(ORDER_QUALIFIER_TYPES, '0.0'),
					qualifierTypes: ORDER_QUALIFIER_TYPES
				})
			case 'item':
				this.setState({
					discountType: value,
    				qualifierType: _.get(ITEM_QUALIFIER_TYPES, '0.0'),
					qualifierTypes: ITEM_QUALIFIER_TYPES
				})
		}
	}

	@autobind
	qualifierTypeChange(value) {
		this.setState({
			qualifierType: value
		})
	}

	@autobind
	offerTypeChange(value) {		
		this.setState({
			offerType: value
		})
	}

	@autobind
	renderQualAddInput() {
		if (this.state.discountType == 'order') {
			switch (this.state.qualifierType) {
				case 'noQualifier':
					return this.renderOrderAddones1();
					break;
				case 'subtotalOfOrder':
					return this.renderOrderAddones2();
					break;			
				case 'number':	
					return this.renderOrderAddones3();
					break;
			}
		}	else if (this.state.discountType == 'item') {
			switch (this.state.qualifierType) {
				case 'noQualifier':
					return this.renderItemAddones1();
					break;
				case 'subtotalOfOrder':
					return this.renderItemAddones2();
					break;			
				case 'number':	
					return this.renderItemAddones3();
					break;
			}
		}
	}

	@autobind
	renderOfferAddInput() {
	}

	@autobind	
	renderOrderAddones1() {
		return null;
	}

	@autobind	
	renderOrderAddones2() {
		return (
			<div>Spend <Currency onChange={this.setOrderAddon2} value={this.state.orderAddon2}/> or more</div>
		);	
	}

	@autobind	
	renderOrderAddones3() {
		return (
			<div>Order <Counter onChange={this.setOrderAddon3} value={this.state.orderAddon3}/> or more</div>
		);	
	}

	@autobind	
	renderItemAddones1() {
		return null;
	}

	@autobind	
	renderItemAddones2() {
		return (
			<div>Spend <Currency onChange={this.setItemAddon2} value={this.state.itemAddon2}/> or more on following items</div>
		);	
	}

	@autobind	
	renderItemAddones3() {
		return (
			<div>Order <Counter onChange={this.setItemAddon3} value={this.state.itemAddon3}/> or more of the following items</div>
		);	
	}

	@autobind
	setOrderAddon2(value) {
		this.setState({
			orderAddon2: value
		})
	}

	@autobind
	setOrderAddon3(value) {
		this.setState({
			orderAddon3: value 
		})
	}
	@autobind
	setItemAddon2(value) {
		this.setState({
			itemAddon2: value
		})
	}

	@autobind
	setItemAddon3(value) {
		this.setState({
			itemAddon3: value 
		})
	}

	@autobind
	renderQueryBuilder() {
		
	}

	@autobind
	toggleExGiftCardQual() {
		this.setState({
			exGiftCardQual: !this.state.exGiftCardQual
		})
	}

	@autobind
	toggleExGiftCardOffer() {
		this.setState({
			exGiftCardOffer: !this.state.exGiftCardOffer
		})
	}

	@autobind
	setPercent(value) {		
		this.setState({
			percent: value
		})
	}

	render(){
		return(
			<div styleName="discount_qualifier">
				<div styleName="sub-title">Qualifier</div>
				<FormField
					className="fc-object-form__field">
					<Checkbox id="isExGiftCardQual" 
						inline 
						checked={this.state.exGiftCardQual} 
						onChange={this.toggleExGiftCardQual}>
            			<label htmlFor="isExGiftCardQual">Exclude gift cards from quaifying criteria</label>
            		</Checkbox>	
				</FormField>
				<Dropdown 
					className="autowidth_dd"
					items={DISCOUNT_TYPES}
					value={this.state.discountType}
					onChange={this.discountTypeChange}/>
				<Dropdown
					className="autowidth_dd"
					items={this.state.qualifierTypes}
					value={this.state.qualifierType}
					onChange={this.qualifierTypeChange}/>
				<div className="inline-container">{this.renderQualAddInput()}</div>
				<div className="">{this.renderQueryBuilder()}</div>
				<div styleName="sub-title">Offer</div>
				<FormField
					className="fc-object-form__field">
					<Checkbox id="isExGiftCardOffer" 
						inline 
						checked={this.state.exGiftCardOffer} 
						onChange={this.toggleExGiftCardOffer}>
            			<label htmlFor="isExGiftCardOffer">Exclude gift cards from discounted items</label>
            		</Checkbox>	
				</FormField>
				<Dropdown 
					className="autowidth_dd"
					items={OFFER_TYPES}
					value={this.state.offerType}
					onChange={this.offerTypeChange}/>
				<div className="inline-container">{this.renderOfferAddInput()}</div>

			</div>
		);
	}
}
