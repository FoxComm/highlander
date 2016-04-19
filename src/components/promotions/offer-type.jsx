
/* @flow weak */

import _ from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import styles from './attrs-edit.css';

import { Dropdown, DropdownItem } from '../dropdown';
import CurrencyInput from '../forms/currency-input';
import AppendInput from '../forms/append-input';
import { Checkbox } from '../checkbox/checkbox';
import SelectVertical from '../select-verical/select-vertical';

import { actions } from '../../modules/products/list';

const offersItems = [
  ['orderPercentOff', 'Percent off order'],
  ['itemsSelectPercentOff', 'Percent off select items'],
  ['freeShipping', 'Free shipping'],
];

type OrderActions = {
  fetchSearches: Function,
};

type Props = {
  onChange: (offer: Object) => any,
  discount: Object,
  productSearches: Array<any>,
  ordersActions: OrderActions,
};

type Target = {
  value: any,
};

type Event = {
  target: Target,
};

type State = {
  discountItemsMode: Object,
};

function mapStateToProps(state) {
  return {
    productSearches: _.get(state, 'products.list.savedSearches', []),
  };
}

function mapDispatchToProps(dispatch) {
  return {
    ordersActions: bindActionCreators(actions, dispatch),
  };
}

/* ::`*/
@connect(mapStateToProps, mapDispatchToProps)
/* ::`*/
export default class Offer extends Component {

  static propTypes = {
    onChange: PropTypes.func.isRequired,
    discount: PropTypes.object,
    productSearches: PropTypes.array,
    ordersActions: PropTypes.shape({
      fetchSearches: PropTypes.func.isRequired,
    }).isRequired,
  };

  state: State = {
    discountItemsMode: this.initialDiscountItemsMode,
  };

  get initialDiscountItemsMode() {
    return _.get(this.offerParams, 'references', []).length > 1 ? 'any' : 'some';
  }

  componentDidMount() {
    this.props.ordersActions.fetchSearches();
  }

  get offer() {
    const { discount } = this.props;
    return _.get(discount, 'form.attributes.offer', {});
  }

  get offerType() {
    return Object.keys(this.offer)[0];
  }

  get offerParams() {
    return this.offer[this.offerType] || {};
  }

  get controlAfterType() {
    switch (this.offerType) {
      case 'orderPercentOff':
        return this.percentOffOrder;
      case 'itemsSelectPercentOff':
        return this.percentOffItems;
      default:
        return null;
    }
  }

  @autobind
  handleDiscountChange(event: Event) {
    let discount = Number(event.target.value);
    discount = isNaN(discount) ? 0 : discount;
    discount = Math.min(100, discount);
    discount = Math.max(0, discount);

    this.setParams({
      discount,
    });
  }

  get percentOffOrder(): Element {
    return (
      <div styleName="control-after-type">
        <span>Get</span>
        <AppendInput
          styleName="inline-edit-input"
          min={0}
          max={100}
          type="number"
          plate="%"
          value={this.offerParams.discount}
          onChange={this.handleDiscountChange}
        />
        <span>off your order.</span>
      </div>
    );
  }

  get percentOffItems(): Element {
    return (
      <div styleName="control-after-type">
        <span>Get</span>
        <AppendInput
          styleName="inline-edit-input"
          min={0}
          max={100}
          type="number"
          plate="%"
          value={this.offerParams.discount}
          onChange={this.handleDiscountChange}
        />
        <span>off discounted items.</span>
      </div>
    );
  }

  setParams(params: Object) {
    this.props.onChange({
      [this.offerType]: {
        ...this.offerParams,
        ...params,
      },
    });
  }

  setType(type: any) {
    this.props.onChange({
      [type]: this.offerParams,
    });
  }

  @autobind
  handleOfferTypeChange(value: any) {
    this.setType(value);
  }

  get content(): ?Element {
    switch (this.offerType) {
      case 'itemsSelectPercentOff':
        return this.itemsSelectPercentOff;
      default:
        return null;
    }
  }

  @autobind
  handleChangeDiscountItemsMode(value: any) {
    this.setState({
      discountItemsMode: value,
    });
    const references = _.get(this.offerParams, 'references', []);
    if (value === 'some' && references.length > 1) {
      this.setParams({
        references: references.slice(0, 1),
      });
    }
  }

  @autobind
  handleSelectReference(id: any) {
    this.handleSelectReferences([id]);
  }

  @autobind
  handleSelectReferences(ids: Array<any>) {
    this.setParams({
      references: ids.map(id => ({referenceId: id, referenceType: 'SavedProductSearch'})),
    });
  }

  get discountReferences(): Element {
    const references = _.get(this.offerParams, 'references', []);

    if (this.state.discountItemsMode == 'some') {
      const productSearches = this.props.productSearches
        .filter(search => search.id != null)
        .map(search => [search.id, search.title]);

      const initialValue = references.length && references[0].referenceId || void 0;

      return (
        <Dropdown
          styleName="wide-dropdown"
          value={initialValue}
          placeholder="- Select Product Search -"
          items={productSearches}
          onChange={this.handleSelectReference}
        />
      );
    } else {
      const productSearches = this.props.productSearches
        .filter(search => search.id != null)
        .reduce((acc, search) => {
          acc[search.id] = search.title;
          return acc;
        }, {});

      const indexedReferences = _.indexBy(references, 'referenceId');

      let counter = 1;
      const initialItems = _.transform(productSearches, (items, title, id) => {
        if (id in indexedReferences) {
          items[counter++] = id;
        }
      }, {});

      return (
        <SelectVertical
          initialItems={initialItems}
          options={productSearches}
          placeholder="- Select Product Search -"
          onChange={this.handleSelectReferences} />
      );
    }
  }

  get itemsSelectPercentOff(): Element {
    return (
      <div>
        <div styleName="discount-items">
          <strong styleName="discount-items-label">Discount the items</strong>
          <Dropdown value={this.state.discountItemsMode} onChange={this.handleChangeDiscountItemsMode}>
            <DropdownItem value="some">in</DropdownItem>
            <DropdownItem value="any">in any of</DropdownItem>
          </Dropdown>
          {this.discountReferences}
        </div>
      </div>
    );
  }

  render(): Element {
    return (
      <div>
        <div styleName="form-row">
          <Dropdown
            styleName="type-chooser"
            items={offersItems}
            value={this.offerType}
            onChange={this.handleOfferTypeChange}
          />
          {this.controlAfterType}
        </div>
        <div styleName="form-row">
          {this.content}
        </div>
      </div>
    );
  }
}
