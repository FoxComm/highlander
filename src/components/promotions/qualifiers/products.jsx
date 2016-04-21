
/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import styles from './products.css';

import SelectVertical from '../../select-verical/select-vertical';
import { Dropdown, DropdownItem } from '../../dropdown';

import { actions } from '../../../modules/products/list';

type OrderActions = {
  fetchSearches: Function,
};

type RefId = string|number;

type Reference = {
  referenceId: RefId;
  referenceType: string;
}

type Props = {
  onChange: (references: Array<Reference>) => any;
  references: Array<Reference>;
  label: string;
  productSearches: Array<any>;
  ordersActions: OrderActions;
}

type State = {
  selectMode: string;
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

  get initialSelectMode() {
    return this.props.references.length > 1 ? 'any' : 'some';
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
    this.props.onChange(ids.map(id => ({referenceId: id, referenceType: 'SavedProductSearch'})));
  }

  get productReferences(): Element {
    const { references } = this.props;

    if (this.state.selectMode == 'some') {
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

  @autobind
  handleChangeSelectMode(value: string) {
    this.setState({
      selectMode: value,
    });
    const { references } = this.props;
    if (value === 'some' && references.length > 1) {
      this.props.onChange(references.slice(0, 1));
    }
  }

  render() {
    return (
      <div styleName="products">
        <strong styleName="label">{this.props.label}</strong>
        <Dropdown value={this.state.selectMode} onChange={this.handleChangeSelectMode}>
          <DropdownItem value="some">in</DropdownItem>
          <DropdownItem value="any">in any of</DropdownItem>
        </Dropdown>
        {this.productReferences}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductsQualifier);
