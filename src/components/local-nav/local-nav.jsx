//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import flatMap from 'lodash.flatmap';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import classNames from 'classnames';

//helpers
import { addResizeListener, removeResizeListener } from '../../lib/resize';
//components
import { Link, IndexLink } from '../link';


class NavDropdown extends React.Component {
  render() {
    const { title, className, children } = this.props;
    const cls = classNames(
      'fc-tabbed-nav-parent',
      'fc-tabbed-nav-item',
      className
    );

    return (
      <li className={cls}>
        <a>{title}</a>
        <ul className="fc-tabbed-nav-dropdown">
          {React.Children.map(children, item => <li>{item}</li>)}
        </ul>
      </li>
    );
  }
}

NavDropdown.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  className: PropTypes.string
};


@connect(state => ({ router: state.router }))
class LocalNav extends React.Component {
  static propTypes = {
    router: PropTypes.shape({
      routes: PropTypes.array
    }),
    children: PropTypes.node,
    gutter: PropTypes.bool,
  };

  static defaultProps = {
    gutter: false
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

  @autobind
  compileLinks({ props }) {
    return flatMap(React.Children.toArray(props.children), child => {
      if (child.type === Link || child.type === IndexLink) {
        return child;
      }
      if (React.isValidElement(child)) {
        return this.compileLinks(child);
      }
      return [];
    });
  }

  @autobind
  hasActiveLink(item) {
    const { routes } = this.props.router;
    const linkList = this.compileLinks(item);
    const linkNames = _.pluck(linkList, ['props', 'to']);

    const currentRoute = routes[routes.length - 1];

    return _.includes(linkNames, currentRoute.name);
  }

  @autobind
  renderItem(item, index) {
    // Index based keys aren't great, but in this case we don't have better
    // information and these won't get reordered - so it's fine.
    const key = `local-nav-item-${item.key ? item.key : index}`;
    if (item.type !== NavDropdown) {
      return <li ref={index} className="fc-tabbed-nav-item" key={key}>{item}</li>;
    }

    const isActive = this.hasActiveLink(item);

    return React.cloneElement(item, {
      ref: index,
      className: classNames(item.props.className, { 'fc-tabbed-nav-selected': isActive }),
      key: key,
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

    const children = React.Children.toArray(this.props.children).slice(collapseFrom);

    return (
      <NavDropdown ref={collapseFrom} title="More">
        {children}
      </NavDropdown>
    );
  }

  render() {
    const { gutter } = this.props;
    const className = classNames('fc-grid', { 'fc-grid-gutter': gutter });

    return (
      <div className={className}>
        <div className="fc-col-md-1-1">
          <ul className="fc-tabbed-nav">
            {this.flatItems}
            {this.collapsedItems}
          </ul>
        </div>
      </div>
    );
  }
}

export {
  LocalNav as default,
  NavDropdown
};
