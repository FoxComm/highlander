
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import { IndexLink, Link } from '../link';

export default class NavigationItem extends React.Component {

  static propTypes = {
    isIndex: PropTypes.boolean,
    isExpandable: PropTypes.boolean,
    to: PropTypes.string,
    icon: PropTypes.string,
    title: PropTypes.string
  }

  static defaultProps = {
    isIndex: false,
    isExpandable: false
  }

  constructor(...args) {
    super(...args);
    this.state = {
      open: false
    };
  }

  @autobind
  expandItem() {
    const status = !this.state.open;
    this.setState({open: status});
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

  render() {
    return this.props.isIndex ? this.indexLink : this.link;
  }
}
