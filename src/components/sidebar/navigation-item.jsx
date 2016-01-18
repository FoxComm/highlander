
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import { IndexLink, Link } from '../link';

export default class NavigationItem extends React.Component {

  static propTypes = {
    isIndex: PropTypes.bool,
    isExpandable: PropTypes.bool,
    collapsed: PropTypes.bool,
    to: PropTypes.string.isRequired,
    icon: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    children: PropTypes.node,
    routes: PropTypes.array.isRequired
  }

  static defaultProps = {
    isIndex: false,
    isExpandable: false,
    collapsed: false
  }

  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      open: this.isOpen(props)
    };
  }

  componentWillReceiveProps(nextProps) {
    if (!nextProps.collapsed) {
      this.setState({open: this.isOpen(nextProps)});
    }
  }

  @autobind
  expandItem() {
    const status = !this.state.open;
    this.setState({open: status});
  }

  @autobind
  isOpen(props) {
    let initialIsOpen = false;
    if (!_.isEmpty(props.children)) {
      const tos = _.compact(props.children.map(c => c.props.to));
      const routeNames = _.compact(props.routes.map(route => route.name));
      initialIsOpen = !_.isEmpty(_.intersection(tos, routeNames));
    }
    return initialIsOpen;
  }

  get expandButton() {
    const iconClass = classNames({
      'icon-chevron-down': !this.state.open,
      'icon-chevron-up': this.state.open
    });
    return (
      <div className="fc-navigation-item__expand" onClick={this.expandItem}>
        <i className={iconClass}></i>
      </div>
    );
  }

  get indexLink() {
    return (
      <div className="fc-navigation-item">
        <IndexLink to={this.props.to} className="fc-navigation-item__link">
          <i className={this.props.icon}></i>
          {this.props.title}
        </IndexLink>
        {this.props.isExpandable && this.expandButton}
      </div>
    );
  }

  get link() {
    return (
      <div className="fc-navigation-item">
        <Link to={this.props.to} className="fc-navigation-item__link">
          <i className={this.props.icon}></i>
          {this.props.title}
        </Link>
        {this.props.isExpandable && this.expandButton}
      </div>
    );
  }

  get childrenClass() {
    return classNames('fc-navigation-item__children', {
      '_open': !this.props.collapsed && this.props.isExpandable && this.state.open
    });
  }

  get containerClass() {
    const routeNames = this.props.routes.map(route => route.name);
    const isActive = _.contains(routeNames, this.props.to) ||
      _.contains(routeNames, `${this.props.to}-base`);
    return classNames('fc-navigation-item-container', {
      '_active': isActive
    });
  }

  render() {
    return (
      <div className={this.containerClass}>
        { this.props.isIndex ? this.indexLink : this.link }
        <div className={this.childrenClass}>
          {this.props.children}
        </div>
      </div>
    );
  }
}
