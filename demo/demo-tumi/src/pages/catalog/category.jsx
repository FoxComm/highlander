/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { categoryNameToUrl, categoryNameFromUrl } from 'paragons/categories';
import { connect } from 'react-redux';
import classNames from 'classnames';

import { Link } from 'react-router';

import styles from './category.css';

const mapStateToProps = (state) => {
  return {
    categories: state.categories.list,
  };
};

class CategoryPage extends Component {

  get category() {
    const { categoryName } = this.props.params;
    return _.find(this.props.categories, { name: categoryName });
  }

  get collections() {
    return _.map(this.category.collections, (collection) => {
      return (
        <div styleName="collection" key={`${_.kebabCase(collection.name)}-collection`}>
          <img src={collection.image} styleName="collection-image" />
          <div styleName="collection-name">
            {collection.name}
          </div>
          <Link to={collection.linkTo} styleName="collection-link">
            Shop Now &gt;
          </Link>
        </div>
      );
    });
  }

  get categoryHead() {
    const category = this.category;
    const name = categoryNameFromUrl(category.name);
    const url = `/c/${categoryNameToUrl(category.name)}`;

    const className = classNames(styles['category-head'], {
      [styles._left]: category.position === 'left',
      [styles._right]: category.position === 'right',
    });

    return (
      <div className={className} >
        <div styleName="category-title">
          {name}
        </div>
        <div styleName="category-desc">
          {category.description}
        </div>
        <Link to={url} styleName="category-link">
          View All
        </Link>
      </div>
    );
  }

  render() {
    const { categoryName } = this.props.params;
    const name = categoryNameFromUrl(categoryName);
    const category = this.category;

    if (!category) return null;

    const style = { backgroundImage: `url(${category.heroImage})` };
    return (
      <div>
        <div styleName="hero-image" style={style}>
          {this.categoryHead}
        </div>
        <div styleName="collections-title">
          Shop {name} by Collection
        </div>
        <div styleName="collections">
          {this.collections}
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, {})(CategoryPage);
