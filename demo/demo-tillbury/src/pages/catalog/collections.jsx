/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { categoryNameFromUrl } from 'paragons/categories';
import { connect } from 'react-redux';
import classNames from 'classnames';

import { Link } from 'react-router';

import styles from './category.css';

const mapStateToProps = (state) => {
  return {
    categories: state.categories.list,
  };
};

class CollectionsPage extends Component {

  get category() {
    return _.find(this.props.categories, { name: 'collections' });
  }

  get collections() {
    return _.map(this.category.collections, (collection) => {
      const className = classNames(styles['collection-text'], styles[`_${collection.color}`], {
        [styles._top]: !collection.titleOnTile,
      });

      const linkClass = classNames(styles['collection-link-tile'], {
        [styles._top]: !collection.titleOnTile,
      });

      const imageClass = classNames(styles['collection-image'], styles[collection.imageClass]);

      const body = collection.titleOnTile ? (
        <Link to={collection.linkTo} className={linkClass}>
          <img src={collection.image} className={imageClass} />
          <div className={className}>
            <div styleName="collection-tile-name">
              {collection.name}
            </div>
            <div styleName="collection-tile-desc">
              {collection.description}
            </div>
          </div>
        </Link>
        ) : (
          <Link to={collection.linkTo} className={linkClass}>
            <div className={className}>
              <div styleName="collection-tile-name">
                {collection.name}
              </div>
              <div styleName="collection-tile-desc">
                {collection.description}
              </div>
            </div>
            <div styleName="image-under-text">
              <img src={collection.image} styleName="collection-image" />
            </div>
          </Link>
        );

      return (
        <div styleName="collection-tile" key={`${_.kebabCase(collection.name)}-collection`}>
          {body}
        </div>
      );
    });
  }

  get categoryHead() {
    const category = this.category;
    const name = categoryNameFromUrl(category.name);

    const className = classNames(styles['category-head'], styles._middle);

    return (
      <div className={className} >
        <div styleName="category-title">
          {name}
        </div>
      </div>
    );
  }

  render() {
    const category = this.category;

    if (!category) return null;

    const style = { backgroundImage: `url(${category.heroImage})` };
    return (
      <div>
        <div styleName="hero-image" style={style}>
          {this.categoryHead}
        </div>
        <div styleName="collection-tiles">
          {this.collections}
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, {})(CollectionsPage);
