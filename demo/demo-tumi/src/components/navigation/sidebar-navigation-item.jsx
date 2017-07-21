/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { Link } from 'react-router';
import { browserHistory } from 'lib/history';

import ActionLink from 'ui/action-link/action-link';

import classNames from 'classnames';

import { humanize, categoryNameToUrl } from 'paragons/categories';

import styles from './sidebar-navigation.css';

type Category = {
  name: string,
  id: number,
  description: string,
  url?: string,
  children?: ?Array<any>,
};

type Props = {
  item: Category,
  path: string,
  level: number,
  onClick: ?Function,
  onGoBack: () => any,
  onGoDeeper: () => any,
  renderBack: Function,
};

type State = {
  expanded: ?string,
};

function menuItemOpened(expanded: ?string, name: string): boolean {
  return expanded === name;
}

function isActiveItem(path: string, itemName: string): boolean {
  const dashedName = _.toLower(itemName.replace(/\s/g, '-'));

  return !!path.match(new RegExp(dashedName, 'i'));
}

export default class NavigationItem extends Component {
  props: Props;
  state: State = {
    expanded: null,
  };

  static defaultProps = {
    level: 0,
  };

  @autobind
  getNavUrl(category: ?Category): string {
    let url;

    if (category === undefined) {
      url = '/';
    } else {
      const dashedName = categoryNameToUrl(category.name);
      url = `/${dashedName}`;
    }

    return url;
  }

  @autobind
  push(url: string) {
    return () => {
      browserHistory.push(url);
      this.setState({ expanded: null }, this.props.onGoBack);
    };
  }

  @autobind
  goBack() {
    this.setState({ expanded: null }, () => {
      this.props.onGoBack();
      this.renderBack();
    });
  }

  @autobind
  renderBack() {
    const fabric = () => {
      if (!this.state.expanded) return null;

      const name = humanize(this.state.expanded);

      return <ActionLink styleName="action-link-back" action={this.goBack} title={name} />;
    };

    this.props.renderBack(fabric);
  }

  @autobind
  drawer(item: Category, parentUrl: string): ?Element<*> {
    const children = _.map(item.children, (child) => {
      const url = `/c${parentUrl}?PRODUCTTYPE=${categoryNameToUrl(child.name).toUpperCase()}`;
      const name = humanize(child.name);
      const styleName = classNames(styles['submenu-link-item'], {
        [styles.active]: isActiveItem(this.props.path, child.name),
      });

      return (
        <Link className={styleName} onClick={this.push(url)} key={url}>
          {name}
        </Link>
      );
    });

    return (
      <div styleName="submenu">
        {children}
      </div>
    );
  }

  @autobind
  goDown(e: SyntheticEvent, expanded: string) {
    e.preventDefault();
    e.stopPropagation();

    this.setState({ expanded }, () => {
      this.renderBack();
      this.props.onGoDeeper();
    });
  }

  render() {
    const { item, path } = this.props;

    if (item.hiddenInNavigation) {
      return null;
    }

    const url = this.getNavUrl(item);
    const name = humanize(item.name);
    const linkClasses = classNames(styles['top-item-link'], {
      [styles.active]: isActiveItem(path, item.name),
    });

    const cls = classNames({
      [styles.opened]: menuItemOpened(this.state.expanded, item.name),
    });

    return (
      <div className={cls}>
        <Link className={linkClasses} onClick={e => this.goDown(e, item.name)}>
          {name}
        </Link>
        {this.drawer(item, url)}
      </div>
    );
  }
}
