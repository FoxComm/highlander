// libs
import _ from 'lodash';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// helpers
import { addResizeListener, removeResizeListener } from 'lib/resize';

// components
import { Link, IndexLink } from 'components/link';
import NavDropdown from './nav-dropdown';

// styles
import s from './local-nav.css';

class LocalNav extends Component {

  static contextTypes = {
    router: PropTypes.object.isRequired,
  };

  static propTypes = {
    children: PropTypes.node,
  };

  state = {
    //index of children, from with the automatic collapse starts
    collapseFrom: null,
  };

  componentDidMount() {
    addResizeListener(this.handleResize);
    this.handleResize();
  }

  componentWillUnmount() {
    removeResizeListener(this.handleResize);
  }

  @autobind
  handleResize() {
    this.collapsing = false;
    this.forceUpdate();
  }

  componentDidUpdate() {
    const refs = Object.values(this.refs);

    if (!global.document || refs.length < 2) {
      return;
    }

    const { collapsing } = this;

    if (refs.length > 1 && this.hasOverflow) {
      this.collapse();
    } else if (!collapsing && this.isCollapsed) {
      this.expand();
    }
  }

  get hasOverflow() {
    const refs = Object.values(this.refs);
    let mostLeft = this.refs[0].getBoundingClientRect().left;

    return Boolean(_.find(refs, (ref) => {
      const left = ref instanceof Element
        ? ref.getBoundingClientRect().left
        : ReactDOM.findDOMNode(ref).getBoundingClientRect().left;

      //if referenced node is visible (not in collapsed menu)
      if (ref.offsetParent !== null && left < mostLeft) {
        return true;
      }
      mostLeft = left;
    }));
  }

  get isCollapsed() {
    return this.state.collapseFrom !== null;
  }

  collapse() {
    this.collapsing = true;
    const refs = Object.values(this.refs);
    const { collapseFrom } = this.state;

    this.setState({
      collapseFrom: collapseFrom === null ? refs.length - 2 : collapseFrom - 1
    });
  }

  expand() {
    const total = React.Children.count(this.props.children);
    const { collapseFrom } = this.state;

    this.setState({
      collapseFrom: total - collapseFrom <= 2 ? null : collapseFrom + 1
    });
  }

  compileLinks({ props }) {
    return _.flatMap(React.Children.toArray(props.children), child => {
      if (child.type === Link || child.type === IndexLink) {
        return child;
      }
      if (React.isValidElement(child)) {
        return this.compileLinks(child);
      }
      return [];
    });
  }

  hasActiveLink(item) {
    const { routes } = this.context.router;
    const linkList = this.compileLinks(item);
    const linkNames = _.map(linkList, ['props', 'to']);

    const currentRoute = routes[routes.length - 1];

    return _.includes(linkNames, currentRoute.name);
  }

  isActiveLink(item) {
    if (item.type !== Link && item.type !== IndexLink) {
      return false;
    }

    const linkName = _.get(item, ['props', 'to']);

    return this.context.router.isActive(linkName);
  }

  @autobind
  renderItem(item, index) {
    // Index based keys aren't great, but in this case we don't have better
    // information and these won't get reordered - so it's fine.
    const key = `local-nav-item-${item.key ? item.key : index}`;

    if (item.type !== NavDropdown) {
      const child = React.cloneElement(item, {
        ref: index,
        key,
        activeClassName: s.activeLink,
      });

      return <li ref={index} className={s.item} key={key}>{child}</li>;
    }

    const isActive = this.hasActiveLink(item);

    return React.cloneElement(item, {
      ref: index,
      className: classNames(item.props.className, { [s.selected]: isActive }),
      key: key,
      activeClassName: s.activeLink,
    });
  }

  get flatItems() {
    const { collapseFrom } = this.state;

    let children = React.Children.toArray(this.props.children);
    if (collapseFrom !== null) {
      children = children.slice(0, collapseFrom);
    }

    return children.map(this.renderItem);
  }

  get collapsedItems() {
    const { collapseFrom } = this.state;

    if (collapseFrom === null) {
      return null;
    }

    const children = React.Children
      .toArray(this.props.children)
      .slice(collapseFrom)
      .map(el => React.cloneElement(el, { activeClassName: s.activeLink }));

    return (
      <NavDropdown ref={collapseFrom} title="More">
        {children}
      </NavDropdown>
    );
  }

  render() {
    const { className } = this.props;

    return (
      <ul className={classNames(s.block, className)}>
        {this.flatItems}
        {this.collapsedItems}
      </ul>
    );
  }
}

export default LocalNav;
