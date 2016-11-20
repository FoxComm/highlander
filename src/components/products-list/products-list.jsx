/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';

// styles
import styles from './products-list.css';

// components
import ListItem from '../products-item/list-item';
import Loader from 'ui/loader';

// types
import type { HTMLElement } from 'types';

type Props = {
  list: ?Array<Object>,
  isLoading: ?boolean,
};

class ProductsList extends Component {
  props: Props;

  getItemList() {
    return _.map(this.props.list, (item) => {
      return (
        <ListItem
          {...item}
          key={`product-${item.id}`}
          ref={`product-${item.id}`}
        />
      );
    });
  }

  render() : HTMLElement {
    const props = this.props;
    const { isLoading } = props;
    const items = props.list && props.list.length > 0
      ? this.getItemList()
      : <div styleName="not-found">No products found.</div>;

    if (isLoading) return <Loader />;

    return (
      <div styleName="list">
        {items}
      </div>
    );
  }
}

export default ProductsList;
