
/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import styles from './select-products.css';

import SelectVertical from '../../select-verical/select-vertical';
import { Dropdown, DropdownItem } from '../../dropdown';

import { actions } from '../../../modules/products/list';
import type { Context } from '../types';

type OrderActions = {
  fetchSearches: Function,
};

type RefId = string|number;

type Props = {
  context: Context,
  label: string;
  name: string;
  productSearches: Array<any>;
  ordersActions: OrderActions;
}

type State = {
  selectMode: string;
};

const SELECT_PRODUCT_ITEMS = [
  ['some', 'in'],
  ['any', 'in any of'],
];

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

class ProductsQualifier extends Component {
  props: Props;

  state: State = {
    selectMode: this.initialSelectMode,
  };

  static propTypes = {
    productSearches: PropTypes.array,
    ordersActions: PropTypes.shape({
      fetchSearches: PropTypes.func.isRequired,
    }).isRequired,
  };

  get references() {
    return _.get(this.props.context.params, this.props.name, []);
  }

  updateReferences(references) {
    this.props.context.setParams({
      [this.props.name]: references,
    });
  }

  get initialSelectMode() {
    return this.references.length > 1 ? 'any' : 'some';
  }

  componentDidMount() {
    this.props.ordersActions.fetchSearches();
  }

  @autobind
  handleSelectReference(id: RefId) {
    this.handleSelectReferences([id]);
  }

  @autobind
  handleSelectReferences(ids: Array<RefId>) {
    this.updateReferences(ids.map(id => ({productSearchId: id})));
  }

  get productReferences(): Element {
    const { references } = this;

    if (this.state.selectMode == 'some') {
      const productSearches = this.props.productSearches
        .filter(search => search.id != null)
        .map(search => [search.id, search.title]);

      const initialValue = references.length && references[0].productSearchId || void 0;

      return (
        <Dropdown
          styleName="wide-dropdown"
          value={initialValue}
          placeholder="- Select Product Search -"
          emptyMessage="There are no saved product searches."
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

      const indexedReferences = _.keyBy(references, 'productSearchId');

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
          emptyMessage="There are no saved product searches."
          onChange={this.handleSelectReferences} />
      );
    }
  }

  @autobind
  handleChangeSelectMode(value: string) {
    this.setState({
      selectMode: value,
    });
    const { references } = this;
    if (value === 'some' && references.length > 1) {
      this.updateReferences(references.slice(0, 1));
    }
  }

  render() {
    return (
      <div styleName="products">
        <strong styleName="label">{this.props.label}</strong>
        <Dropdown styleName="mode-dropdown"
                  value={this.state.selectMode}
                  onChange={this.handleChangeSelectMode}
                  items={SELECT_PRODUCT_ITEMS}
        />
        {this.productReferences}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductsQualifier);


