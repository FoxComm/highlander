/* @flow */

// libs
import React from 'react';

// components
import SvgIcon from 'components/core/svg-icon';

// styles
import s from './svg-icons.css';

type Props = {
  iconSet: Array<string>
};

const SvgIcons = (props: Props) => {
  const { iconSet, ...rest } = props;
  const icons = iconSet.map((iconType) => (
    <div key={iconType} className={s.box}>
      <div className={s.iconWrapper}>
        <SvgIcon name={iconType} {...rest} className={s.svgStyle} />
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

export default SvgIcons;
