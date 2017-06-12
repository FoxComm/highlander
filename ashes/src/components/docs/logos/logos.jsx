/* @flow */

// libs
import React from 'react';

// components
import SvgIcon from 'components/core/svg-icon';

// styles
import s from './logos.css';

type Props = {
  logoSet: Array<string>
};

const Logos = (props: Props) => {
  const { logoSet, ...rest } = props;
  const icons = logoSet.map((logoType) => (
    <div key={logoType} className={s.box}>
      <div className={s.iconWrapper}>
        <SvgIcon name={logoType} {...rest} className={s.svgStyle} />
      </div>
      <span className={s.text}>{logoType}</span>
    </div>
  ));

  return (
    <div className={s.container}>
      {icons}
    </div>
  );
};

export default Logos;
