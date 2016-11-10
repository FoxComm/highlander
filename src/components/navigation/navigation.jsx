/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { Link } from 'react-router';

import localized from 'lib/i18n';

import * as actions from 'modules/categories';

import styles from './navigation.css';

type Category = {
  name: string,
  id: number,
  description: string,
  url?: string,
};

type Props = {
  list: Array<any>,
  fetch: Function,
  hasAllLink: boolean,
  onClick?: ?Function,
  t: any,
};

const getState = state => ({...state.categories});

class Navigation extends Component {
  props: Props;

  static defaultProps = {
    hasAllLink: false,
  };

  componentWillMount() {
    this.props.fetch();
  }

  get allLink() {
    const { t } = this.props;

    if (this.props.hasAllLink) {
      return (
        <li styleName="item" key="category-all">
          <Link to="/" styleName="item-link" onClick={this.props.onClick}>{t('ALL')}</Link>
        </li>
      );
    }
  }

  @autobind
  getNavUrl(category : ?Category) {
    let url;

    if (category == undefined) {
      url = '/';
    } else {
      const dashedName = category.name.replace(/\s/g, '-');
      url = `/${dashedName}`;
    }

    return url;
  }

  render(): HTMLElement {
    const { t } = this.props;

    const categoryItems = _.map(this.props.list, (item) => {
      const dashedName = item.name.replace(/\s/g, '-');
      const key = `category-${dashedName}`;
      const url = this.getNavUrl(item);

      return (
        <li styleName="item" key={key}>
          <Link styleName="item-link" to={url} onClick={this.props.onClick}>
            {t(item.name.toUpperCase())}
          </Link>
        </li>
      );
    });

    return (
      <ul styleName="list">
        {this.allLink}
        {categoryItems}
      </ul>
    );
  }
}

export default connect(getState, actions)(localized(Navigation));
