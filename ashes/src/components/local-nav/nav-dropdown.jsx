import React, { PropTypes, Component } from 'react';
import classNames from 'classnames';

class NavDropdown extends Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    className: PropTypes.string
  };

  render() {
    const { title, className, children } = this.props;
    const cls = classNames(
      'fc-tabbed-nav-parent',
      'fc-tabbed-nav-item',
      className
    );

    return (
      <li className={cls}>
        <a>{title}<i className="icon-chevron-down"/></a>
        <ul className="fc-tabbed-nav-dropdown">
          {React.Children.map(children, item => <li>{item}</li>)}
        </ul>
      </li>
    );
  }
}

export default NavDropdown;
