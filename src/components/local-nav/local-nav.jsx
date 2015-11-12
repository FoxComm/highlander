import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

const NavDropdown = props => {
  return (
    <li className={`fc-tabbed-nav-parent fc-tabbed-nav-item ${props.className || ''}`}>
      <a>{props.title}</a>
      <ul className="fc-tabbed-nav-dropdown">
        {React.Children.map(props.children, item => <li>{item}</li>)}
      </ul>
    </li>
  );
};

NavDropdown.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired
};

@connect(state => ({router: state.router}))
class LocalNav extends React.Component {
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
    if (item.type === NavDropdown) {
      const isActive = this.hasActiveLink(item);
      const dropdownItem = React.cloneElement(item, {
        className: isActive ? 'fc-tabbed-nav-selected' : ''
      });
      return dropdownItem;
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
  children: PropTypes.node,
  gutter: PropTypes.bool
};

LocalNav.defaultProps = {
  gutter: false
};

export {
  LocalNav as default,
  NavDropdown
};
