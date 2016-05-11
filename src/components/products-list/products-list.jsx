/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import { connect } from 'react-redux';

import styles from './products-list.css';

import ListItem from '../products-item/list-item';
import Banner from '../banner/banner';

type Category = {
  name: string;
  id: number;
  description: string;
};

type ProductsListParams = {
  list: ?Array<Product>;
  categories: ?Array<Category>;
  category: ?string;
  location: any;
  hasBanners: boolean;
}

const mapStateToProps = state => ({categories: state.categories.list});

class ProductsList extends React.Component {
  props: ProductsListParams;

  static defaultProps = {
    hasBanners: true
  };

  renderHeader() {
    const props = this.props;

    if (!props.category) return;

    const category = _.find(props.categories, {name: props.category});
    const description = (category && category.description) ? <p styleName="description">{category.description}</p> : '';

    let className = `header-${props.category}`;
    let title = props.category;

    if (props.location.query && props.location.query.type) {
      className = `${className}-${props.location.query.type}`;
      title = `${props.location.query.type}'s ${title}`;
    }

    return (
      <header styleName={className}>
        <div styleName="header-wrap">
          <h1 styleName="title">{title}</h1>
          {description}
        </div>
      </header>
    );
  }

  getItemList() {
    const items = _.map(this.props.list, (item, i) => {
        return (
          <ListItem {...item} key={`product-${item.id}`}/>
        );
    });

    if (!this.props.hasBanners) return items;

    const banner1 = <div styleName="banner-sunglasses" key="banner-sunglasses">
      <Banner
        header="Summer 2016"
        description="Bring on the sun"
        links={[{to: '/sunglasses', text: 'Shop Sunglasses'}]}
      />
      <div styleName="banner-sunglasses__image"></div>
    </div>;

    const banner2 = <div styleName="banner-eyeglasses" key="banner-eyeglasses">
      <Banner
        header="Summer 2016"
        description="Better to see you with, my dear"
        links={[{to: '/eyeglasses', text: 'Shop Eyeglasses'}]}
      />
      <div styleName="banner-eyeglasses__image"></div>
    </div>;

    if (items.length > 6) items.splice(6, 0, banner1);
    if (items.length > 13) items.splice(13, 0, banner2);

    return items;
  }

  render() : HTMLElement {
    const props = this.props;
    const items = props.list && props.list.length > 0
      ? this.getItemList()
      : <div styleName="not-found">No products found.</div>;

    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="list">
          {items}
        </div>
      </section>
    );
  }
}

export default connect(mapStateToProps, {})(ProductsList);
