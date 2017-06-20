// @flow

// libs
import React from 'react';
import classNames from 'classnames';
import Transition from 'react-transition-group/CSSTransitionGroup';

// styles
import s from './menu.css';

type MenuProps = {
  children?: any;
  position?: 'left' | 'center' | 'right';
  isOpen?: boolean;
};

export const Menu = (props: MenuProps) => {
  const { position = 'left', isOpen = false, children } = props;

  const transitionProps = {
    component: 'div',
    transitionName: `dd-transition-${position}`,
    transitionEnterTimeout: 300,
    transitionLeaveTimeout: 300,
  };

  return (
    <Transition {...transitionProps}>
      {isOpen &&
      <div className={s.block}>{children}</div>
      }
    </Transition>

  );
};

type MenuItemProps = {
  className?: string;
  children?: any;
  clickAction?: Function;
  active?: boolean;
};

const preventBlur = event => {
  event.preventDefault();
  event.stopPropagation();
};

export const MenuItem = (props: MenuItemProps) => {
  const { className, clickAction, children, active } = props;
  const klass = classNames(s.item, className, { [s.active]: active });

  const click = event => {
    event.preventDefault();
    event.stopPropagation();

    if (clickAction) {
      clickAction(event);
    }
  };

  return (
    <div
      className={klass}
      onClick={click}
      onMouseDown={preventBlur}
      onMouseUp={preventBlur}
    >
      {children}
    </div>
  );
};
