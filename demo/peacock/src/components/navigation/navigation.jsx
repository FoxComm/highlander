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

class NavigationItem extends Component {
  state = {
    expanded: false,
  };

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

  @autobind
  handleHoverOn() {
    console.log('on');
    this.setState({ expanded: true });
  }

  @autobind
  handleHoverOff() {
    console.log('off');
    this.setState({ expanded: false });
  }

  render() {
    const { item, path, t } = this.props;

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

    const drawerStyle = classNames(styles.submenu, {
      [styles.open]: this.state.expanded,
    });

    return (
      <div
        className={linkClasses}
        key={key}
        onMouseEnter={this.handleHoverOn}
        onMouseLeave={this.handleHoverOff}
      >
        <Link
          styleName="item-link"
          to={url}
          onClick={this.props.onClick}
        >
          {t(item.name.toUpperCase())}
        </Link>
        { item.children && <div className={drawerStyle}>Sumbenu { item.name }</div> }
      </div>
    );
  }
}

const getState = state => ({...state.categories});

class Navigation extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetch();
  }

  render(): Element<*> {
    const { t } = this.props;
    const path = decodeURIComponent(this.props.path);

    const categoryItems = _.map(this.props.list, (item) => {
      return (
        <NavigationItem item={item} path={this.props.path} t={this.props.t}/>
      );
    });

    return (
      <div styleName="list">
        {categoryItems}
      </div>
    );
  }
}

export default connect(getState, actions)(localized(Navigation));
