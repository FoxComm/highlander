/* @flow */

import React, { PropTypes } from 'react';
import type { HTMLElement } from 'types';
import _ from 'lodash';
import styles from './categories.css';
import cssModules from 'react-css-modules';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

import * as actions from 'modules/categories';

type Category = {
  name: string;
  id: number;
};

const getState = state => ({ list: state.categories.list });

class Categories extends React.Component {

  static propTypes = {
    list: PropTypes.array,
    fetchCategories: PropTypes.func.isRequired,
    onClick: PropTypes.func,
  };

  static defaultProps = {
    onClick: _.noop,
  };

  componentDidMount() {
    this.props.fetchCategories();
  }

  @autobind
  onClick(category : ?Category) {
    this.props.onClick(category);
    if (category == undefined) {
      browserHistory.push('/');
    } else {
      const dashedName = category.name.replace(/\s/g, '-');
      browserHistory.push(`/${category.id}-${dashedName}`);
    }
  }

  render(): HTMLElement {
    const categoryItems = _.map(this.props.list, (item) => {
      const dashedName = item.name.replace(/\s/g, '-');
      const key = `category-${dashedName}`;
      return (
        <div styleName="item" key={key}>
          <a onClick={() => this.onClick(item)} styleName="item-link">
            {item.name.toUpperCase()}
          </a>
        </div>
      );
    });

    return (
      <div styleName="list">
        <div styleName="item" key="category-all">
          <a onClick={() => this.onClick()} styleName="item-link">ALL</a>
        </div>
        {categoryItems}
      </div>
    );
  }
}

export default connect(getState, actions)(cssModules(Categories, styles));
