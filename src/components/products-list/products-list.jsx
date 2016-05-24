/* @flow */

import _ from 'lodash';
import React from 'react';
import { findDOMNode } from 'react-dom';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import { connect } from 'react-redux';

import styles from './products-list.css';

import ListItem from '../products-item/list-item';
import BannerWithImage from '../banner/bannerWithImage';
import ScrollToTop from '../scroll-to-top/scroll-to-top';
import ViewIndicator from '../view-indicator/view-indicator';

type Category = {
  name: string;
  id: number;
  description: string;
};

type ProductsListParams = {
  list: ?Array<Product>;
  categories: ?Array<Category>;
  category: ?string;
  categoryType: ?string;
  hasBanners: boolean;
}

type State = {
  viewedItems: number;
}

const mapStateToProps = state => ({categories: state.categories.list});

class ProductsList extends React.Component {
  props: ProductsListParams;

  static defaultProps = {
    hasBanners: true,
  };

  state: State = {
    viewedItems: 0,
  };

  countViewedItems = () => {
    let viewedItems = 0;

    for (const item in this.refs) {
      if (this.refs.hasOwnProperty(item)) {
        const product = this.refs[item];
        const productRect = findDOMNode(product).getBoundingClientRect();
        const windowHeight = window.innerHeight;

        if (productRect.bottom < windowHeight) viewedItems++;
      }
    }

    this.setState({viewedItems});
  };

  renderHeader() {
    const props = this.props;
    const categoryName = props.category;

    if (!categoryName) return;

    const categoryInfo = _.find(props.categories, {name: categoryName});
    const description = (categoryInfo && categoryInfo.description)
      ? <p styleName="description">{categoryInfo.description}</p>
      : '';

    let className = `header-${categoryName}`;
    let title = categoryName;

    if (props.categoryType) {
      className = `${className}-${props.categoryType}`;
      title = `${props.categoryType}'s ${title}`;
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
    const items = _.map(this.props.list, (item) => {
      return (
        <ListItem {...item} key={`product-${item.id}`} ref={`product-${item.id}`}/>
      );
    });

    if (!this.props.hasBanners) return items;

    const bannersData = [
      {
        styleName: 'banner-sunglasses',
        header: 'Summer 2016',
        description: 'Bring on the sun',
        links: [{to: '/collections/summer2016', text: 'Shop Sunglasses'}],
      },
      {
        styleName: 'banner-eyeglasses',
        header: 'Summer 2016',
        description: 'Better to see you with, my dear',
        links: [{to: '/collections/summer2016', text: 'Shop Eyeglasses'}],
      },
    ];

    const banners = bannersData.map((banner, i) => {
      return <BannerWithImage { ...banner } key={`banner-${i}`}/>;
    });

    if (items.length > 6) items.splice(6, 0, banners[0]);
    if (items.length > 13) items.splice(13, 0, banners[1]);

    return items;
  }

  render() : HTMLElement {
    const props = this.props;
    const items = props.list && props.list.length > 0
      ? this.getItemList()
      : <div styleName="not-found">No products found.</div>;

    const totalItems = this.props.list ? this.props.list.length : 0;

    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="list">
          {items}
        </div>
        <ScrollToTop />
        <ViewIndicator
          totalItems={totalItems}
          viewedItems={this.state.viewedItems}
          countViewedItems={this.countViewedItems}
        />
      </section>
    );
  }
}

export default connect(mapStateToProps, {})(ProductsList);
