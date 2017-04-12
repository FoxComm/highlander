// libs
import React, { PropTypes, Component } from 'react';
import classNames from 'classnames';

// styles
import s from './local-nav.css';

class NavDropdown extends Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    className: PropTypes.string
  };

  render() {
    const { title, className, children } = this.props;
    const cls = classNames(
      s.parent,
      s.item,
      className
    );

    return (
      <li className={cls}>
        <a>{title}<i className="icon-chevron-down"/></a>
        <ul className={s.dropdown}>
          {React.Children.map(children, item => <li>{item}</li>)}
        </ul>
      </li>
    );
  }
}

export default NavDropdown;
