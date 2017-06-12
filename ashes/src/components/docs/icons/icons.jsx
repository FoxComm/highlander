/* @flow */

// libs
import React from 'react';

// components
import Icon from 'components/core/icon';

// styles
import s from './icons.css';

type Props = {
  iconSet: Array<string>
};

const Icons = (props: Props) => {
  const icons = props.iconSet.map((iconType) => (
    <div key={iconType} className={s.box}>
      <div className={s.iconWrapper}>
        <Icon name={iconType} />
      </div>
      <span className={s.text}>{iconType}</span>
    </div>
  ));

  return (
    <div className={s.container}>
      {icons}
    </div>
  );
};

export default Icons;
