/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { categoryNameToUrl, categoryNameFromUrl, humanize } from 'paragons/categories';
import { connect } from 'react-redux';
import classNames from 'classnames';

import { Link } from 'react-router';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';

import styles from './category.css';

const mapStateToProps = (state) => {
  return {
    categories: state.categories.list,
  };
};

class CategoryPage extends Component {

  get category() {
    const { categoryName } = this.props.params;
    return _.find(this.props.categories, (cat) => {
      return _.toUpper(categoryNameToUrl(cat.name)) == _.toUpper(categoryNameToUrl(categoryName));
    });
  }

  get blocks() {
    const baseUrl = `/c/${categoryNameToUrl(this.category.name)}`;
    return _.map(this.category.children, (block) => {
      const blockUrl = `${baseUrl}/${categoryNameToUrl(block.name)}`;
      const images = _.map(block.images, url => (<img src={url} />));
      const subs = _.map(block.children, (sub) => {
        return (
          <div styleName="sub">
            <Link to={`${blockUrl}/${categoryNameToUrl(sub.name)}`} styleName="sub-url">
              {humanize(sub.name)}
            </Link>
          </div>
        );
      });
      return (
        <div styleName="category-block">
          <div styleName="links">
            <div styleName="title">{humanize(block.name)}</div>
            {subs}
          </div>
          <div styleName="images">
            {images}
          </div>
        </div>
      );
    });
  }

  render() {
    const { categoryName } = this.props.params;
    const name = categoryNameFromUrl(categoryName);
    const category = this.category;

    if (!category) return null;

    return (
      <div styleName="category">
        <Breadcrumbs
          routes={this.props.routes}
          params={this.props.routerParams}
        />
        {this.blocks}
      </div>
    );
  }
}

export default connect(mapStateToProps, {})(CategoryPage);
