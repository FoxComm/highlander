/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { Link } from 'react-router';

import localized from 'lib/i18n';
import classNames from 'classnames';

import * as actions from 'modules/categories';
import { convertCategoryNameToUrlPart } from 'modules/categories';

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
  onClick?: ?Function,
  t: any,
  path: string,
};

const getState = state => ({...state.categories});

class Navigation extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetch();
  }

  @autobind
  getNavUrl(category : ?Category) {
    let url;

    if (category == undefined) {
      url = '/';
    } else {
      const dashedName = convertCategoryNameToUrlPart(category.name);
      url = `/${dashedName}`;
    }

    return url;
  }

  render(): Element<*> {
    const { t } = this.props;
    const path = decodeURIComponent(this.props.path);

    const categoryItems = _.map(this.props.list, (item) => {
      if (item.hiddenInNavigation) {
        return null;
      }

      const dashedName = item.name.replace(/\s/g, '-');
      const key = `category-${dashedName}`;
      const url = this.getNavUrl(item);
      const isActive = path.match(new RegExp(dashedName, 'i'));
      const linkClasses = classNames(styles.item, {
        [styles.active]: isActive,
      });

      return (
        <li className={linkClasses} key={key}>
          <Link styleName="item-link" to={url} onClick={this.props.onClick}>
            {t(item.name.toUpperCase())}
          </Link>
        </li>
      );
    });

    return (
      <ul styleName="list">
        {categoryItems}
      </ul>
    );
  }
}

export default connect(getState, actions)(localized(Navigation));
