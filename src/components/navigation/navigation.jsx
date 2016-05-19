/* @flow */

import React, { PropTypes } from 'react';
import type { HTMLElement } from 'types';
import _ from 'lodash';
import { Link } from 'react-router';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

import localized from 'lib/i18n';

import * as actions from 'modules/categories';

import styles from './navigation.css';

type Category = {
  name: string;
  id: number;
  description: string;
};

const getState = state => ({...state.categories});

class Navigation extends React.Component {

  static propTypes = {
    list: PropTypes.array,
    fetch: PropTypes.func.isRequired,
    onClick: PropTypes.func,
    all: PropTypes.bool,
  };

  static defaultProps = {
    onClick: _.noop,
    all: false,
  };

  componentWillMount() {
    this.props.fetch();
  }

  @autobind
  onClick(category : ?Category, type : ?string) {
    this.props.onClick(category);
    if (category == undefined) {
      browserHistory.push('/');
    } else {
      const dashedName = category.name.replace(/\s/g, '-');
      if (type) {
        browserHistory.push({pathname: `/${dashedName}`, query: {type}});
      } else {
        browserHistory.push(`/${dashedName}`);
      }
    }
  }

  render(): HTMLElement {
    const { t } = this.props;

    const categoryItems = _.map(this.props.list, (item) => {
      const dashedName = item.name.replace(/\s/g, '-');
      const key = `category-${dashedName}`;
      return (
        <li styleName="item" key={key}>
          <a styleName="item-link">
          {t(item.name.toUpperCase())}
          </a>
          <ul>
            <li><a onClick={() => this.onClick(item, 'men')}>MEN'S</a></li>
            <li><a onClick={() => this.onClick(item, 'women')}>WOMEN'S</a></li>
          </ul>
        </li>
      );
    });

    return (
      <ul styleName="list">
        {this.props.all && (
          <li styleName="item" key="category-all">
            <a onClick={() => this.onClick()} styleName="item-link">{t('ALL')}</a>
          </li>
        )}
        {categoryItems}
        <li styleName="item" ><Link to="/locations" styleName="item-link">Locations</Link></li>
        <li styleName="item" ><Link to="/our-story" styleName="item-link">Our story</Link></li>
      </ul>
    );
  }
}

export default connect(getState, actions)(localized(Navigation));
