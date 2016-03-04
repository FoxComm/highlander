/* @flow */

import React, { PropTypes } from 'react';
import _ from 'lodash';
import styles from './categories.css';
import cssModules from 'react-css-modules';
import { connect } from 'react-redux';
import type { HTMLElement } from '../../types';

import * as actions from '../../modules/categories';

const getState = state => ({ list: state.categories.list });

class Categories extends React.Component {

  static propTypes = {
    list: PropTypes.array,
    fetchCategories: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.fetchCategories();
  }

  render(): HTMLElement {
    const categoryItems = _.map(this.props.list, (item) => {
      const key = `category-${item.name.replace(/\s/g, '-')}`;
      return (
        <div styleName="item" key={key}>
          <a href="#" styleName="item-link">{item.name.toUpperCase()}</a>
        </div>
      );
    });

    return (
      <div styleName="list">
        {categoryItems}
      </div>
    );
  }
}

export default connect(getState, actions)(cssModules(Categories, styles));
