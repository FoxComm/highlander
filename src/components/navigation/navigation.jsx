/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

import localized from 'lib/i18n';

import * as actions from 'modules/categories';

import styles from './navigation.css';

type Category = {
  name: string,
  id: number,
  description: string,
};

type Props = {
  list: Array<any>,
  fetch: Function,
  onClick: Function,
  hasAllLink: boolean,
  t: any,
};

const getState = state => ({...state.categories});

class Navigation extends Component {
  props: Props;

  static defaultProps = {
    onClick: _.noop,
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
          <a onClick={() => this.onClick()} styleName="item-link">{t('ALL')}</a>
        </li>
      );
    }
  }

  @autobind
  onClick(category : ?Category) {
    this.props.onClick(category);
    if (category == undefined) {
      browserHistory.push('/');
    } else {
      const dashedName = category.name.replace(/\s/g, '-');
      browserHistory.push(`/${dashedName}`);
    }
  }

  render(): HTMLElement {
    const { t } = this.props;

    const categoryItems = _.map(this.props.list, (item) => {
      const dashedName = item.name.replace(/\s/g, '-');
      const key = `category-${dashedName}`;
      return (
        <li styleName="item" key={key}>
          <a styleName="item-link" onClick={() => this.onClick(item)}>
            {t(item.name.toUpperCase())}
          </a>
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
