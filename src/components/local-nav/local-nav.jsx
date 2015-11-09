'use strict';

import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

@connect(state => ({router: state.router}))
export default class LocalNav extends React.Component {
  static propTypes = {
    router: PropTypes.shape({
      routes: PropTypes.array
    }),
    children: PropTypes.node
  };
  @autobind
  compileLinks(item) {
    let linkList = [];
    let children = item.props.children;
    if (!_.isArray(children)) children = [children];

    for (let child of children) {
      if (child.type === Link) {
        linkList.push(child);
      } else if (_.isArray(child.props.children)) {
        linkList.push(...this.compileLinks(child));
      } else if (_.isObject(child.props.children)) {
        linkList.push(...this.compileLinks(child.props.children));
      }
    }
    return linkList;
  }

  @autobind
  hasActiveLink(item) {
    const linkList = this.compileLinks(item);
    const linkNames = _.pluck(linkList, ['props', 'to']);

    const currentRoute = this.props.router.routes[this.props.router.routes.length - 1];
    return _.includes(linkNames, currentRoute.name);
  }

  @autobind
  renderItem(item) {
    if (item.type === 'li') {
      let isActive = this.hasActiveLink(item);
      const parentItem = React.cloneElement(item, {
        className: `fc-tabbed-nav-parent fc-tabbed-nav-item ${isActive ? 'fc-tabbed-nav-selected' : ''}`
      });
      return parentItem;
    } else {
      return <li className="fc-tabbed-nav-item">{item}</li>;
    }
  }

  render() {
    return (
      <div className={`fc-grid ${this.props.gutter ? 'fc-grid-gutter' : ''}`}>
        <div className="fc-col-md-1-1">
          <ul className="fc-tabbed-nav">
            {React.Children.map(this.props.children, this.renderItem)}
          </ul>
        </div>
      </div>
    );
  }
}

LocalNav.propTypes = {
  gutter: PropTypes.bool
};

LocalNav.defaultProps = {
  gutter: false
};
